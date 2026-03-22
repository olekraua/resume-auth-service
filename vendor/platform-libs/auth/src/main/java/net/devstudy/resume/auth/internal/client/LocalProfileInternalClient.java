package net.devstudy.resume.auth.internal.client;

import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileIdentifierLookupRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileLookupResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileUidUpdateRequest;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.service.ProfileReadService;
import net.devstudy.resume.profile.api.service.ProfileService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.services.profile.mode", havingValue = "local", matchIfMissing = true)
public class LocalProfileInternalClient implements ProfileInternalClient {

    private final ProfileService profileService;
    private final ProfileReadService profileReadService;

    @Override
    public ProfileAuthResponse register(ProfileRegistrationRequest request) {
        Profile profile = profileService.register(request.uid(), request.firstName(), request.lastName(),
                request.password());
        return toAuth(profile);
    }

    @Override
    public ProfileLookupResponse lookup(ProfileIdentifierLookupRequest request) {
        String identifier = request.identifier();
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String trimmed = identifier.trim();
        String lower = trimmed.toLowerCase(Locale.ENGLISH);
        Profile profile = profileReadService.findByUid(lower)
                .or(() -> profileReadService.findByEmail(lower))
                .or(() -> profileReadService.findByPhone(trimmed))
                .orElse(null);
        return profile == null ? null : toLookup(profile);
    }

    @Override
    public void updateUid(Long profileId, ProfileUidUpdateRequest request) {
        profileService.updateUid(profileId, request.uid());
    }

    @Override
    public boolean uidExists(String uid) {
        return profileReadService.uidExists(normalize(uid));
    }

    @Override
    public void removeProfile(Long profileId) {
        profileService.removeProfile(profileId);
    }

    private ProfileAuthResponse toAuth(Profile profile) {
        return new ProfileAuthResponse(
                profile.getId(),
                profile.getUid(),
                null,
                profile.getFirstName(),
                profile.getLastName(),
                profile.getEmail(),
                profile.getPhone()
        );
    }

    private ProfileLookupResponse toLookup(Profile profile) {
        return new ProfileLookupResponse(
                profile.getId(),
                profile.getUid(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getFirstName(),
                profile.getLastName()
        );
    }

    private String normalize(String uid) {
        return uid == null ? null : uid.trim().toLowerCase(Locale.ENGLISH);
    }
}
