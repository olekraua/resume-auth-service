package net.devstudy.resume.ms.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.devstudy.resume.ms.auth.adapters.profile.client.ProfileInternalClient;
import net.devstudy.resume.ms.auth.application.service.impl.UidSuggestionServiceImpl;

@ExtendWith(MockitoExtension.class)
class UidSuggestionServiceImplTest {

    @Mock
    private ProfileInternalClient profileInternalClient;

    @Test
    void suggestShouldReturnNormalizedCandidateForDeterministicAlphabet() {
        UidSuggestionServiceImpl service = new UidSuggestionServiceImpl(profileInternalClient, 5, "a", 2);
        when(profileInternalClient.uidExists("john-aa")).thenReturn(false);

        List<String> result = service.suggest("  John ");

        assertThat(result).containsExactly("john-aa");
        verify(profileInternalClient, times(5)).uidExists("john-aa");
    }

    @Test
    void suggestShouldReturnEmptyWhenAllCandidatesExist() {
        UidSuggestionServiceImpl service = new UidSuggestionServiceImpl(profileInternalClient, 3, "a", 2);
        when(profileInternalClient.uidExists("john-aa")).thenReturn(true);

        List<String> result = service.suggest("john");

        assertThat(result).isEmpty();
        verify(profileInternalClient, times(3)).uidExists("john-aa");
    }

    @Test
    void suggestShouldReturnEmptyForInvalidInputsOrConfig() {
        UidSuggestionServiceImpl invalidSuffixLength =
                new UidSuggestionServiceImpl(profileInternalClient, 10, "abc", 0);
        UidSuggestionServiceImpl emptyAlphabet =
                new UidSuggestionServiceImpl(profileInternalClient, 10, "", 2);

        assertThat(invalidSuffixLength.suggest(null)).isEmpty();
        assertThat(invalidSuffixLength.suggest("   ")).isEmpty();
        assertThat(invalidSuffixLength.suggest("john")).isEmpty();
        assertThat(emptyAlphabet.suggest("john")).isEmpty();

        verifyNoInteractions(profileInternalClient);
    }

    @Test
    void suggestShouldReturnEmptyWhenUidLengthExceedsLimit() {
        String baseUidWithLength49 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        UidSuggestionServiceImpl service = new UidSuggestionServiceImpl(profileInternalClient, 5, "abc", 2);

        List<String> result = service.suggest(baseUidWithLength49);

        assertThat(result).isEmpty();
        verifyNoInteractions(profileInternalClient);
    }
}
