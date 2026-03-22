package net.devstudy.resume.ms.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import net.devstudy.resume.ms.auth.application.port.in.service.ProfileAccountService;
import net.devstudy.resume.ms.auth.adapters.profile.client.ProfileInternalClient;
import net.devstudy.resume.ms.auth.domain.entity.ProfileRestore;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.ProfileRestoreRepository;
import net.devstudy.resume.ms.auth.application.service.impl.RestoreAccessServiceImpl;
import net.devstudy.resume.ms.auth.ports.notification.event.RestoreAccessMailRequestedEvent;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileIdentifierLookupRequest;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileLookupResponse;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfilePasswordUpdateRequest;
import net.devstudy.resume.ms.auth.application.support.component.DataBuilder;

@ExtendWith(MockitoExtension.class)
class RestoreAccessServiceImplTest {

    @Mock
    private ProfileInternalClient profileInternalClient;

    @Mock
    private ProfileAccountService profileAccountService;

    @Mock
    private ProfileRestoreRepository profileRestoreRepository;

    @Mock
    private DataBuilder dataBuilder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RestoreAccessServiceImpl restoreAccessService;

    @BeforeEach
    void setUp() {
        restoreAccessService = new RestoreAccessServiceImpl(
                profileInternalClient,
                profileAccountService,
                profileRestoreRepository,
                dataBuilder,
                eventPublisher,
                Duration.ofHours(1)
        );
    }

    @Test
    void requestRestoreShouldReturnFakeLinkWhenProfileNotFound() {
        when(dataBuilder.buildRestoreAccessLink(eq("https://app.local"), any(String.class)))
                .thenAnswer(invocation -> "https://app.local/restore/" + invocation.getArgument(1, String.class));
        when(profileInternalClient.lookup(any(ProfileIdentifierLookupRequest.class))).thenReturn(null);

        String link = restoreAccessService.requestRestore(" user@example.com ", "https://app.local");

        assertThat(link).startsWith("https://app.local/restore/");
        String token = link.substring(link.lastIndexOf('/') + 1);
        assertThat(token).hasSize(32);
        verify(profileRestoreRepository, never()).save(any(ProfileRestore.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void requestRestoreShouldPersistHashedTokenAndPublishEvent() {
        when(dataBuilder.buildRestoreAccessLink(eq("https://app.local"), any(String.class)))
                .thenAnswer(invocation -> "https://app.local/restore/" + invocation.getArgument(1, String.class));
        ProfileLookupResponse profile = new ProfileLookupResponse(
                5L,
                "john-doe",
                "user@example.com",
                "+12025550123",
                "John",
                "Doe"
        );
        when(profileInternalClient.lookup(any(ProfileIdentifierLookupRequest.class))).thenReturn(profile);
        when(profileRestoreRepository.findByProfileId(5L)).thenReturn(Optional.empty());
        when(profileRestoreRepository.save(any(ProfileRestore.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ProfileRestore.class));

        String link = restoreAccessService.requestRestore("john-doe", "https://app.local");

        ArgumentCaptor<ProfileRestore> restoreCaptor = ArgumentCaptor.forClass(ProfileRestore.class);
        verify(profileRestoreRepository).save(restoreCaptor.capture());
        ProfileRestore saved = restoreCaptor.getValue();
        assertThat(saved.getProfileId()).isEqualTo(5L);
        assertThat(saved.getToken()).hasSize(64).matches("^[0-9a-f]{64}$");
        assertThat(saved.getCreated()).isNotNull();
        assertThat(saved.getToken()).doesNotContain("-");

        ArgumentCaptor<RestoreAccessMailRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(RestoreAccessMailRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        RestoreAccessMailRequestedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.email()).isEqualTo("user@example.com");
        assertThat(publishedEvent.firstName()).isEqualTo("John");
        assertThat(publishedEvent.link()).isEqualTo(link);
    }

    @Test
    void findProfileByTokenShouldReturnProfileIdWhenTokenValid() {
        String token = "validToken123";
        ProfileRestore restore = new ProfileRestore(42L, sha256(token), Instant.now().minusSeconds(30));
        when(profileRestoreRepository.findByToken(sha256(token))).thenReturn(Optional.of(restore));

        Optional<Long> result = restoreAccessService.findProfileByToken("  " + token + " ");

        assertThat(result).contains(42L);
        verify(profileRestoreRepository, never()).delete(any(ProfileRestore.class));
    }

    @Test
    void findProfileByTokenShouldMigrateLegacyPlainToken() {
        String token = "legacyToken";
        ProfileRestore legacy = new ProfileRestore(7L, token, Instant.now().minusSeconds(10));
        when(profileRestoreRepository.findByToken(sha256(token))).thenReturn(Optional.empty());
        when(profileRestoreRepository.findByToken(token)).thenReturn(Optional.of(legacy));
        when(profileRestoreRepository.save(any(ProfileRestore.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, ProfileRestore.class));

        Optional<Long> result = restoreAccessService.findProfileByToken(token);

        assertThat(result).contains(7L);
        verify(profileRestoreRepository).save(legacy);
        assertThat(legacy.getToken()).isEqualTo(sha256(token));
    }

    @Test
    void resetPasswordShouldUpdateAndDeleteWhenTokenValid() {
        String token = "restoreToken";
        ProfileRestore restore = new ProfileRestore(12L, sha256(token), Instant.now().minusSeconds(20));
        when(profileRestoreRepository.findByToken(sha256(token))).thenReturn(Optional.of(restore));

        restoreAccessService.resetPassword(token, "newPassword123");

        verify(profileAccountService).updatePassword(
                eq(12L),
                eq(new ProfilePasswordUpdateRequest("newPassword123"))
        );
        verify(profileRestoreRepository).delete(restore);
    }

    @Test
    void resetPasswordShouldDeleteExpiredTokenAndThrow() {
        String token = "expired";
        ProfileRestore restore = new ProfileRestore(21L, sha256(token), Instant.now().minus(Duration.ofHours(2)));
        when(profileRestoreRepository.findByToken(sha256(token))).thenReturn(Optional.of(restore));

        assertThatThrownBy(() -> restoreAccessService.resetPassword(token, "newPassword123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Невірний токен");

        verify(profileRestoreRepository).delete(restore);
        verify(profileAccountService, never()).updatePassword(any(Long.class), any(ProfilePasswordUpdateRequest.class));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
