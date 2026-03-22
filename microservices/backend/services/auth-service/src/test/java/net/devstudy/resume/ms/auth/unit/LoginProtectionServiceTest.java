package net.devstudy.resume.ms.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import net.devstudy.resume.ms.auth.domain.entity.AuthUser;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.AuthUserRepository;
import net.devstudy.resume.ms.auth.application.security.LoginLockedException;
import net.devstudy.resume.ms.auth.application.security.LoginProtectionService;

@ExtendWith(MockitoExtension.class)
class LoginProtectionServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    private LoginProtectionService loginProtectionService;

    @BeforeEach
    void setUp() {
        loginProtectionService = new LoginProtectionService(authUserRepository);
        ReflectionTestUtils.setField(loginProtectionService, "enabled", true);
        ReflectionTestUtils.setField(loginProtectionService, "maxFailedAttemptsBeforeLockout", 3);
        ReflectionTestUtils.setField(loginProtectionService, "resetWindow", Duration.ofMinutes(30));
        ReflectionTestUtils.setField(loginProtectionService, "initialBackoff", Duration.ofSeconds(30));
        ReflectionTestUtils.setField(loginProtectionService, "maxBackoff", Duration.ofMinutes(30));
    }

    @Test
    void assertLoginAllowedShouldThrowWhenUserLocked() {
        AuthUser authUser = authUser("john");
        authUser.setLoginLockedUntil(Instant.now().plusSeconds(45));
        when(authUserRepository.findByUid("john")).thenReturn(Optional.of(authUser));

        LoginLockedException ex = assertThrows(
                LoginLockedException.class,
                () -> loginProtectionService.assertLoginAllowed("  John ")
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getRetryAfterSeconds()).isBetween(1L, 45L);
        verify(authUserRepository).findByUid("john");
    }

    @Test
    void onAuthenticationFailureShouldApplyBackoffAfterThreshold() {
        AuthUser authUser = authUser("john");
        authUser.setFailedLoginAttempts(3);
        authUser.setLastFailedLoginAt(Instant.now().minusSeconds(10));
        when(authUserRepository.findByUidForUpdate("john")).thenReturn(Optional.of(authUser));

        loginProtectionService.onAuthenticationFailure(" JOHN ");

        verify(authUserRepository).save(authUser);
        assertThat(authUser.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(authUser.getLoginLockedUntil()).isNotNull();
        long backoffSeconds = Duration.between(authUser.getLastFailedLoginAt(), authUser.getLoginLockedUntil())
                .toSeconds();
        assertThat(backoffSeconds).isBetween(29L, 31L);
    }

    @Test
    void onAuthenticationFailureShouldResetCounterAfterWindow() {
        AuthUser authUser = authUser("john");
        authUser.setFailedLoginAttempts(10);
        authUser.setLastFailedLoginAt(Instant.now().minus(Duration.ofHours(2)));
        when(authUserRepository.findByUidForUpdate("john")).thenReturn(Optional.of(authUser));

        loginProtectionService.onAuthenticationFailure("john");

        verify(authUserRepository).save(authUser);
        assertThat(authUser.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(authUser.getLoginLockedUntil()).isNull();
    }

    @Test
    void onAuthenticationSuccessShouldResetProtectionState() {
        AuthUser authUser = authUser("john");
        authUser.setFailedLoginAttempts(2);
        authUser.setLastFailedLoginAt(Instant.now().minusSeconds(20));
        authUser.setLoginLockedUntil(Instant.now().plusSeconds(20));
        when(authUserRepository.findByUidForUpdate("john")).thenReturn(Optional.of(authUser));

        loginProtectionService.onAuthenticationSuccess(" john ");

        verify(authUserRepository).save(authUser);
        assertThat(authUser.getFailedLoginAttempts()).isZero();
        assertThat(authUser.getLastFailedLoginAt()).isNull();
        assertThat(authUser.getLoginLockedUntil()).isNull();
    }

    @Test
    void shouldDoNothingWhenFeatureDisabled() {
        ReflectionTestUtils.setField(loginProtectionService, "enabled", false);

        loginProtectionService.assertLoginAllowed("john");
        loginProtectionService.onAuthenticationSuccess("john");
        loginProtectionService.onAuthenticationFailure("john");

        verifyNoInteractions(authUserRepository);
    }

    @Test
    void shouldIgnoreBlankUsername() {
        loginProtectionService.assertLoginAllowed("   ");
        loginProtectionService.onAuthenticationSuccess("\n");
        loginProtectionService.onAuthenticationFailure(null);

        verify(authUserRepository, never()).findByUidForUpdate("john");
        verifyNoInteractions(authUserRepository);
    }

    private AuthUser authUser(String uid) {
        AuthUser authUser = new AuthUser();
        authUser.setId(1L);
        authUser.setUid(uid);
        authUser.setPasswordHash("hash");
        authUser.setFirstName("John");
        authUser.setLastName("Doe");
        authUser.setCreated(Instant.now());
        return authUser;
    }
}
