package net.devstudy.resume.ms.auth.application.port.in.service;

import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfilePasswordUpdateRequest;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileUidUpdateRequest;

public interface ProfileAccountService {

    ProfileAuthResponse register(ProfileRegistrationRequest request);

    ProfileAuthResponse loadForAuth(String uid);

    void updatePassword(Long profileId, ProfilePasswordUpdateRequest request);

    void updateUid(Long profileId, ProfileUidUpdateRequest request);

    void removeProfile(Long profileId);
}
