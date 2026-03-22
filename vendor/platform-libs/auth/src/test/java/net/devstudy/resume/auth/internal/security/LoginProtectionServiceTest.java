package net.devstudy.resume.auth.internal.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import net.devstudy.resume.auth.internal.entity.AuthUser;
import net.devstudy.resume.auth.internal.repository.storage.AuthUserRepository;

@ExtendWith(MockitoExtension.class)
class LoginProtectionServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    private LoginProtectionService loginProtectionService;

    @BeforeEach
    void setUp() {
        loginProtectionService = new LoginProtectionService(authUserRepository);
        ReflectionTestUtils.setField(loginProtectionService, "enabled", true);
        ReflectionTestUtils.setField(loginProtectionService, "maxFailedAttemptsBeforeLockout", 5);
        ReflectionTestUtils.setField(loginProtectionService, "resetWindow", Duration.ofMinutes(30));
        ReflectionTestUtils.setField(loginProtectionService, "initialBackoff", Duration.ofSeconds(30));
        ReflectionTestUtils.setField(loginProtectionService, "maxBackoff", Duration.ofMinutes(30));
    }

    @Test
    void shouldSetLockAfterThresholdExceeded() {
        AuthUser authUser = new AuthUser();
        authUser.setUid("demo-user");
        when(authUserRepository.findByUidForUpdate(anyString())).thenReturn(Optional.of(authUser));

        for (int i = 0; i < 5; i++) {
            loginProtectionService.onAuthenticationFailure("demo-user");
        }

        assertEquals(5, authUser.getFailedLoginAttempts());
        assertNull(authUser.getLoginLockedUntil());

        loginProtectionService.onAuthenticationFailure("demo-user");

        assertEquals(6, authUser.getFailedLoginAttempts());
        assertNotNull(authUser.getLoginLockedUntil());
        assertTrue(authUser.getLoginLockedUntil().isAfter(Instant.now()));
    }

    @Test
    void shouldResetProtectionStateAfterSuccessfulAuthentication() {
        AuthUser authUser = new AuthUser();
        authUser.setUid("demo-user");
        authUser.setFailedLoginAttempts(7);
        authUser.setLastFailedLoginAt(Instant.now().minusSeconds(10));
        authUser.setLoginLockedUntil(Instant.now().plusSeconds(120));
        when(authUserRepository.findByUidForUpdate(anyString())).thenReturn(Optional.of(authUser));

        loginProtectionService.onAuthenticationSuccess("demo-user");

        assertEquals(0, authUser.getFailedLoginAttempts());
        assertNull(authUser.getLastFailedLoginAt());
        assertNull(authUser.getLoginLockedUntil());
    }

    @Test
    void shouldThrowLockedExceptionWithRetryAfter() {
        AuthUser authUser = new AuthUser();
        authUser.setUid("demo-user");
        authUser.setLoginLockedUntil(Instant.now().plusSeconds(20));
        when(authUserRepository.findByUid(anyString())).thenReturn(Optional.of(authUser));

        LoginLockedException ex = assertThrows(LoginLockedException.class,
                () -> loginProtectionService.assertLoginAllowed("demo-user"));

        assertTrue(ex.getRetryAfterSeconds() >= 1);
    }
}
