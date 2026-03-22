package net.devstudy.resume.auth.internal.service.impl;

import java.time.Instant;
import java.util.Locale;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.api.service.ProfileAccountService;
import net.devstudy.resume.auth.internal.client.ProfileInternalClient;
import net.devstudy.resume.auth.internal.entity.AuthUser;
import net.devstudy.resume.auth.internal.repository.storage.AuthUserRepository;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfilePasswordUpdateRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileUidUpdateRequest;
import net.devstudy.resume.profile.api.event.ProfilePasswordChangedEvent;

@Service
@RequiredArgsConstructor
public class ProfileAccountServiceImpl implements ProfileAccountService {

    private final ProfileInternalClient profileInternalClient;
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ProfileAuthResponse register(ProfileRegistrationRequest request) {
        ProfileAuthResponse profile = profileInternalClient.register(request);
        if (profile == null || profile.id() == null || !StringUtils.hasText(profile.uid())) {
            throw new IllegalStateException("Profile registration failed");
        }
        AuthUser authUser = new AuthUser();
        authUser.setId(profile.id());
        authUser.setUid(normalize(profile.uid()));
        authUser.setPasswordHash(passwordEncoder.encode(request.password()));
        authUser.setFirstName(request.firstName());
        authUser.setLastName(request.lastName());
        authUser.setCreated(Instant.now());
        authUser.setEnabled(true);
        try {
            authUserRepository.save(authUser);
        } catch (DataIntegrityViolationException ex) {
            try {
                profileInternalClient.removeProfile(profile.id());
            } catch (Exception ignored) {
                // ignore rollback failure, original exception is more important
            }
            throw ex;
        }
        return new ProfileAuthResponse(
                authUser.getId(),
                authUser.getUid(),
                authUser.getPasswordHash(),
                authUser.getFirstName(),
                authUser.getLastName(),
                profile.email(),
                profile.phone()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileAuthResponse loadForAuth(String uid) {
        String normalized = normalize(uid);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return authUserRepository.findByUid(normalized)
                .map(authUser -> new ProfileAuthResponse(
                        authUser.getId(),
                        authUser.getUid(),
                        authUser.getPasswordHash(),
                        authUser.getFirstName(),
                        authUser.getLastName(),
                        null,
                        null
                ))
                .orElse(null);
    }

    @Override
    @Transactional
    public void updatePassword(Long profileId, ProfilePasswordUpdateRequest request) {
        if (profileId == null) {
            throw new IllegalArgumentException("Profile id required");
        }
        AuthUser authUser = authUserRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        authUser.setPasswordHash(passwordEncoder.encode(request.password()));
        authUserRepository.save(authUser);
        eventPublisher.publishEvent(new ProfilePasswordChangedEvent(profileId));
    }

    @Override
    @Transactional
    public void updateUid(Long profileId, ProfileUidUpdateRequest request) {
        if (profileId == null) {
            throw new IllegalArgumentException("Profile id required");
        }
        profileInternalClient.updateUid(profileId, request);
        AuthUser authUser = authUserRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        authUser.setUid(normalize(request.uid()));
        authUserRepository.save(authUser);
    }

    @Override
    @Transactional
    public void removeProfile(Long profileId) {
        if (profileId == null) {
            return;
        }
        profileInternalClient.removeProfile(profileId);
        authUserRepository.deleteById(profileId);
    }

    private String normalize(String uid) {
        if (!StringUtils.hasText(uid)) {
            return null;
        }
        return uid.trim().toLowerCase(Locale.ENGLISH);
    }
}
