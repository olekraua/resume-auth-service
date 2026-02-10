package net.devstudy.resume.auth.api.service;

import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfilePasswordUpdateRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileUidUpdateRequest;

public interface ProfileAccountService {

    ProfileAuthResponse register(ProfileRegistrationRequest request);

    ProfileAuthResponse loadForAuth(String uid);

    void updatePassword(Long profileId, ProfilePasswordUpdateRequest request);

    void updateUid(Long profileId, ProfileUidUpdateRequest request);

    void removeProfile(Long profileId);
}
