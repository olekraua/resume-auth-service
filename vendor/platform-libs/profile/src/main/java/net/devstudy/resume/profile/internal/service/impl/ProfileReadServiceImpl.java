package net.devstudy.resume.profile.internal.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.internal.repository.storage.ProfileRepository;
import net.devstudy.resume.profile.api.service.ProfileReadService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileReadServiceImpl implements ProfileReadService {

    private final ProfileRepository profileRepository;

    @Override
    public Optional<Profile> findByUid(String uid) {
        if (uid == null) {
            return Optional.empty();
        }
        return profileRepository.findByUid(uid);
    }

    @Override
    public Optional<Profile> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return profileRepository.findByEmail(email);
    }

    @Override
    public Optional<Profile> findByPhone(String phone) {
        if (phone == null) {
            return Optional.empty();
        }
        return profileRepository.findByPhone(phone);
    }

    @Override
    public Optional<Profile> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return profileRepository.findById(id);
    }

    @Override
    public List<Profile> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return profileRepository.findAllById(ids);
    }

    @Override
    public Page<Profile> findAll(Pageable pageable) {
        Pageable effective = pageable == null ? Pageable.unpaged() : pageable;
        return profileRepository.findAll(effective);
    }

    @Override
    public List<Profile> findAllForIndexing() {
        return profileRepository.findAll(Pageable.unpaged()).getContent();
    }

    @Override
    public boolean uidExists(String uid) {
        if (uid == null || uid.isBlank()) {
            return false;
        }
        return profileRepository.countByUid(uid) > 0;
    }
}
