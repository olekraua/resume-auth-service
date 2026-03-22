package net.devstudy.resume.ms.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;

import net.devstudy.resume.ms.auth.application.port.in.service.ProfileAccountService;
import net.devstudy.resume.ms.auth.domain.entity.RememberMeToken;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.RememberMeTokenRepository;
import net.devstudy.resume.ms.auth.application.service.impl.RememberMeService;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileAuthResponse;

@ExtendWith(MockitoExtension.class)
class RememberMeServiceTest {

    @Mock
    private RememberMeTokenRepository rememberMeTokenRepository;

    @Mock
    private ProfileAccountService profileAccountService;

    private RememberMeService rememberMeService;

    @BeforeEach
    void setUp() {
        rememberMeService = new RememberMeService(
                rememberMeTokenRepository,
                profileAccountService,
                Duration.ofDays(14)
        );
    }

    @Test
    void createNewTokenShouldPersistWhenProfileExists() {
        PersistentRememberMeToken token = new PersistentRememberMeToken(
                " John ",
                "series-1",
                "token-1",
                Date.from(Instant.now().minusSeconds(20))
        );
        ProfileAuthResponse profile = new ProfileAuthResponse(
                100L,
                "john",
                "{bcrypt}hash",
                "John",
                "Doe",
                "john@example.com",
                "+12025550123"
        );
        when(profileAccountService.loadForAuth("John")).thenReturn(profile);
        when(rememberMeTokenRepository.findBySeries("series-1")).thenReturn(Optional.empty());

        rememberMeService.createNewToken(token);

        ArgumentCaptor<RememberMeToken> entityCaptor = ArgumentCaptor.forClass(RememberMeToken.class);
        verify(rememberMeTokenRepository).save(entityCaptor.capture());
        RememberMeToken saved = entityCaptor.getValue();
        assertThat(saved.getSeries()).isEqualTo("series-1");
        assertThat(saved.getToken()).isEqualTo("token-1");
        assertThat(saved.getProfileId()).isEqualTo(100L);
        assertThat(saved.getUsername()).isEqualTo("john");
        assertThat(saved.getLastUsed()).isEqualTo(token.getDate().toInstant());
    }

    @Test
    void createNewTokenShouldIgnoreWhenProfileMissing() {
        PersistentRememberMeToken token = new PersistentRememberMeToken(
                "john",
                "series-1",
                "token-1",
                new Date()
        );
        when(profileAccountService.loadForAuth("john")).thenReturn(null);

        rememberMeService.createNewToken(token);

        verify(rememberMeTokenRepository, never()).save(any(RememberMeToken.class));
    }

    @Test
    void updateTokenShouldUpdateExistingSeries() {
        RememberMeToken existing = new RememberMeToken(
                "series-1",
                "old-token",
                Instant.now().minusSeconds(300),
                100L,
                "john"
        );
        Date lastUsed = Date.from(Instant.now());
        when(rememberMeTokenRepository.findBySeries("series-1")).thenReturn(Optional.of(existing));

        rememberMeService.updateToken("series-1", "new-token", lastUsed);

        verify(rememberMeTokenRepository).save(existing);
        assertThat(existing.getToken()).isEqualTo("new-token");
        assertThat(existing.getLastUsed()).isEqualTo(lastUsed.toInstant());
    }

    @Test
    void getTokenForSeriesShouldDeleteExpiredToken() {
        RememberMeToken expired = new RememberMeToken(
                "series-1",
                "token-1",
                Instant.now().minus(Duration.ofDays(20)),
                100L,
                "john"
        );
        when(rememberMeTokenRepository.findBySeries("series-1")).thenReturn(Optional.of(expired));

        PersistentRememberMeToken result = rememberMeService.getTokenForSeries("series-1");

        assertThat(result).isNull();
        verify(rememberMeTokenRepository).delete(expired);
    }

    @Test
    void getTokenForSeriesShouldReturnValidToken() {
        RememberMeToken valid = new RememberMeToken(
                "series-1",
                "token-1",
                Instant.now().minus(Duration.ofHours(2)),
                100L,
                "john"
        );
        when(rememberMeTokenRepository.findBySeries("series-1")).thenReturn(Optional.of(valid));

        PersistentRememberMeToken result = rememberMeService.getTokenForSeries("series-1");

        assertThat(result).isNotNull();
        assertThat(result.getSeries()).isEqualTo("series-1");
        assertThat(result.getTokenValue()).isEqualTo("token-1");
        assertThat(result.getUsername()).isEqualTo("john");
    }

    @Test
    void removeUserTokensShouldTrimUsername() {
        rememberMeService.removeUserTokens(" john ");
        verify(rememberMeTokenRepository).deleteByUsername("john");
    }

    @Test
    void removeUserTokensShouldIgnoreBlankUsername() {
        rememberMeService.removeUserTokens(" ");
        verifyNoInteractions(rememberMeTokenRepository);
    }
}
