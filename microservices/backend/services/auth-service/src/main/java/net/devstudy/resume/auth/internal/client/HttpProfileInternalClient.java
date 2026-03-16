package net.devstudy.resume.auth.internal.client;

import org.springframework.http.HttpStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileIdentifierLookupRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileLookupResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileUidUpdateRequest;

@Component
@ConditionalOnProperty(name = "app.services.profile.mode", havingValue = "remote")
@RequiredArgsConstructor
public class HttpProfileInternalClient implements ProfileInternalClient {

    private final RestClient profileInternalRestClient;

    @Override
    public ProfileAuthResponse register(ProfileRegistrationRequest request) {
        return profileInternalRestClient.post()
                .uri("/internal/profiles")
                .body(request)
                .retrieve()
                .body(ProfileAuthResponse.class);
    }

    @Override
    public ProfileLookupResponse lookup(ProfileIdentifierLookupRequest request) {
        try {
            return profileInternalRestClient.post()
                    .uri("/internal/profiles/lookup")
                    .body(request)
                    .retrieve()
                    .body(ProfileLookupResponse.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw ex;
        }
    }

    @Override
    public void updateUid(Long profileId, ProfileUidUpdateRequest request) {
        profileInternalRestClient.put()
                .uri("/internal/profiles/{id}/uid", profileId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public boolean uidExists(String uid) {
        try {
            Boolean result = profileInternalRestClient.get()
                    .uri("/internal/profiles/exists/uid/{uid}", uid)
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw ex;
        }
    }

    @Override
    public void removeProfile(Long profileId) {
        profileInternalRestClient.delete()
                .uri("/internal/profiles/{id}", profileId)
                .retrieve()
                .toBodilessEntity();
    }
}
