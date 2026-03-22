package net.devstudy.resume.ms.auth.application.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.ms.auth.domain.entity.AuthUser;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.AuthUserRepository;

@Service
@RequiredArgsConstructor
public class LoginProtectionService {

    private static final Duration DEFAULT_RESET_WINDOW = Duration.ofMinutes(30);
    private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofSeconds(30);
    private static final Duration DEFAULT_MAX_BACKOFF = Duration.ofMinutes(30);

    private final AuthUserRepository authUserRepository;

    @Value("${app.security.login-protection.enabled:true}")
    private boolean enabled;

    @Value("${app.security.login-protection.max-failed-attempts-before-lockout:5}")
    private int maxFailedAttemptsBeforeLockout;

    @Value("${app.security.login-protection.reset-window:PT30M}")
    private Duration resetWindow;

    @Value("${app.security.login-protection.initial-backoff:PT30S}")
    private Duration initialBackoff;

    @Value("${app.security.login-protection.max-backoff:PT30M}")
    private Duration maxBackoff;

    @Transactional(readOnly = true)
    public void assertLoginAllowed(String username) {
        if (!enabled) {
            return;
        }
        String normalized = normalize(username);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        authUserRepository.findByUid(normalized).ifPresent(authUser -> {
            Instant lockedUntil = authUser.getLoginLockedUntil();
            Instant now = Instant.now();
            if (lockedUntil != null && lockedUntil.isAfter(now)) {
                throw new LoginLockedException(toRetryAfterSeconds(now, lockedUntil));
            }
        });
    }

    @Transactional
    public void onAuthenticationSuccess(String username) {
        if (!enabled) {
            return;
        }
        String normalized = normalize(username);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        authUserRepository.findByUidForUpdate(normalized).ifPresent(authUser -> {
            if (isLoginProtectionResetRequired(authUser)) {
                resetLoginProtection(authUser);
                authUserRepository.save(authUser);
            }
        });
    }

    @Transactional
    public void onAuthenticationFailure(String username) {
        if (!enabled) {
            return;
        }
        String normalized = normalize(username);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        authUserRepository.findByUidForUpdate(normalized).ifPresent(authUser -> {
            Instant now = Instant.now();
            Duration normalizedResetWindow = normalizeDuration(resetWindow, DEFAULT_RESET_WINDOW);
            int failedAttempts = Math.max(0, authUser.getFailedLoginAttempts());
            Instant lastFailedLoginAt = authUser.getLastFailedLoginAt();
            if (lastFailedLoginAt == null || lastFailedLoginAt.plus(normalizedResetWindow).isBefore(now)) {
                failedAttempts = 0;
            }
            failedAttempts++;

            authUser.setFailedLoginAttempts(failedAttempts);
            authUser.setLastFailedLoginAt(now);

            Duration backoff = calculateBackoff(failedAttempts);
            if (backoff.isZero()) {
                authUser.setLoginLockedUntil(null);
            } else {
                authUser.setLoginLockedUntil(now.plus(backoff));
            }
            authUserRepository.save(authUser);
        });
    }

    private boolean isLoginProtectionResetRequired(AuthUser authUser) {
        return authUser.getFailedLoginAttempts() > 0
                || authUser.getLastFailedLoginAt() != null
                || authUser.getLoginLockedUntil() != null;
    }

    private void resetLoginProtection(AuthUser authUser) {
        authUser.setFailedLoginAttempts(0);
        authUser.setLastFailedLoginAt(null);
        authUser.setLoginLockedUntil(null);
    }

    private Duration calculateBackoff(int failedAttempts) {
        int threshold = Math.max(1, maxFailedAttemptsBeforeLockout);
        int backoffStep = failedAttempts - threshold;
        if (backoffStep <= 0) {
            return Duration.ZERO;
        }

        Duration currentBackoff = normalizeDuration(initialBackoff, DEFAULT_INITIAL_BACKOFF);
        Duration maxAllowedBackoff = normalizeDuration(maxBackoff, DEFAULT_MAX_BACKOFF);
        if (currentBackoff.compareTo(maxAllowedBackoff) >= 0) {
            return maxAllowedBackoff;
        }

        for (int i = 1; i < backoffStep; i++) {
            long currentMillis = currentBackoff.toMillis();
            long maxMillis = maxAllowedBackoff.toMillis();
            if (currentMillis >= maxMillis) {
                return maxAllowedBackoff;
            }
            if (currentMillis >= maxMillis / 2) {
                return maxAllowedBackoff;
            }
            currentBackoff = currentBackoff.multipliedBy(2);
        }
        if (currentBackoff.compareTo(maxAllowedBackoff) > 0) {
            return maxAllowedBackoff;
        }
        return currentBackoff;
    }

    private Duration normalizeDuration(Duration value, Duration fallback) {
        if (value == null || value.isNegative() || value.isZero()) {
            return fallback;
        }
        return value;
    }

    private long toRetryAfterSeconds(Instant now, Instant lockedUntil) {
        long millis = Duration.between(now, lockedUntil).toMillis();
        if (millis <= 0) {
            return 1L;
        }
        return (millis + 999L) / 1000L;
    }

    private String normalize(String uid) {
        if (!StringUtils.hasText(uid)) {
            return null;
        }
        return uid.trim().toLowerCase(Locale.ENGLISH);
    }
}
