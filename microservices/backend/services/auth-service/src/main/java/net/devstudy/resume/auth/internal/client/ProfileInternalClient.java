package net.devstudy.resume.auth.internal.client;

import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileIdentifierLookupRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileLookupResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileUidUpdateRequest;

public interface ProfileInternalClient {

    ProfileAuthResponse register(ProfileRegistrationRequest request);

    ProfileLookupResponse lookup(ProfileIdentifierLookupRequest request);

    void updateUid(Long profileId, ProfileUidUpdateRequest request);

    boolean uidExists(String uid);

    void removeProfile(Long profileId);
}
