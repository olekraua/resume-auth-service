package net.devstudy.resume.ms.auth.adapters.profile.client;

import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileIdentifierLookupRequest;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileLookupResponse;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileUidUpdateRequest;

public interface ProfileInternalClient {

    ProfileAuthResponse register(ProfileRegistrationRequest request);

    ProfileLookupResponse lookup(ProfileIdentifierLookupRequest request);

    void updateUid(Long profileId, ProfileUidUpdateRequest request);

    boolean uidExists(String uid);

    void removeProfile(Long profileId);
}
