package net.devstudy.resume.auth.internal.service.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.devstudy.resume.shared.component.DataBuilder;
import net.devstudy.resume.auth.api.service.ProfileAccountService;
import net.devstudy.resume.auth.internal.client.ProfileInternalClient;
import net.devstudy.resume.auth.internal.entity.ProfileRestore;
import net.devstudy.resume.notification.api.event.RestoreAccessMailRequestedEvent;
import net.devstudy.resume.auth.internal.repository.storage.ProfileRestoreRepository;
import net.devstudy.resume.profile.api.dto.internal.ProfileIdentifierLookupRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileLookupResponse;
import net.devstudy.resume.auth.api.service.RestoreAccessService;

@Service
public class RestoreAccessServiceImpl implements RestoreAccessService {

    private final ProfileInternalClient profileInternalClient;
    private final ProfileAccountService profileAccountService;
    private final ProfileRestoreRepository profileRestoreRepository;
    private final DataBuilder dataBuilder;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration tokenTtl;

    public RestoreAccessServiceImpl(ProfileInternalClient profileInternalClient,
            ProfileAccountService profileAccountService,
            ProfileRestoreRepository profileRestoreRepository,
            DataBuilder dataBuilder,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.restore.token-ttl:PT1H}") Duration tokenTtl) {
        this.profileInternalClient = profileInternalClient;
        this.profileAccountService = profileAccountService;
        this.profileRestoreRepository = profileRestoreRepository;
        this.dataBuilder = dataBuilder;
        this.eventPublisher = eventPublisher;
        this.tokenTtl = tokenTtl;
    }

    @Override
    @Transactional
    public String requestRestore(String identifier, String appHost) {
        ProfileLookupResponse profile = findProfileByIdentifier(identifier).orElse(null);
        if (profile == null) {
            return dataBuilder.buildRestoreAccessLink(appHost, generateToken());
        }

        ProfileRestore restore = profileRestoreRepository.findByProfileId(profile.id())
                .orElseGet(ProfileRestore::new);
        String token = generateToken();
        restore.setProfileId(profile.id());
        restore.setToken(hashToken(token));
        restore.setCreated(Instant.now());
        profileRestoreRepository.save(restore);

        String link = dataBuilder.buildRestoreAccessLink(appHost, token);
        publishRestoreMail(profile, link);
        return link;
    }

    @Override
    @Transactional
    public Optional<Long> findProfileByToken(String token) {
        Optional<ProfileRestore> restore = findRestoreByToken(token);
        if (restore.isEmpty()) {
            return Optional.empty();
        }
        ProfileRestore existing = restore.get();
        if (isExpired(existing, Instant.now())) {
            profileRestoreRepository.delete(existing);
            return Optional.empty();
        }
        return Optional.ofNullable(existing.getProfileId());
    }

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void resetPassword(String token, String rawPassword) {
        ProfileRestore restore = findRestoreByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Невірний токен відновлення"));
        if (isExpired(restore, Instant.now())) {
            profileRestoreRepository.delete(restore);
            throw new IllegalArgumentException("Невірний токен відновлення");
        }
        profileAccountService.updatePassword(restore.getProfileId(),
                new net.devstudy.resume.profile.api.dto.internal.ProfilePasswordUpdateRequest(rawPassword));
        profileRestoreRepository.delete(restore);
    }

    private Optional<ProfileLookupResponse> findProfileByIdentifier(String identifier) {
        if (identifier == null) {
            return Optional.empty();
        }
        String trimmed = identifier.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        ProfileLookupResponse response = profileInternalClient.lookup(
                new ProfileIdentifierLookupRequest(trimmed));
        return Optional.ofNullable(response);
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isExpired(ProfileRestore restore, Instant now) {
        Instant created = restore.getCreated();
        if (created == null) {
            return true;
        }
        return created.plus(tokenTtl).isBefore(now);
    }

    private Optional<ProfileRestore> findRestoreByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String trimmed = token.trim();
        String hashed = hashToken(trimmed);
        Optional<ProfileRestore> restore = profileRestoreRepository.findByToken(hashed);
        if (restore.isPresent()) {
            return restore;
        }
        Optional<ProfileRestore> legacy = profileRestoreRepository.findByToken(trimmed);
        if (legacy.isPresent()) {
            ProfileRestore existing = legacy.get();
            existing.setToken(hashed);
            return Optional.of(profileRestoreRepository.save(existing));
        }
        return Optional.empty();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private void publishRestoreMail(ProfileLookupResponse profile, String link) {
        if (profile == null || link == null || link.isBlank()) {
            return;
        }
        String email = profile.email();
        if (email == null || email.isBlank()) {
            return;
        }
        eventPublisher.publishEvent(new RestoreAccessMailRequestedEvent(email,
                profile.firstName(),
                link));
    }
}
