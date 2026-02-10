package net.devstudy.resume.profile.api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.devstudy.resume.profile.api.model.Profile;

public interface ProfileReadService {
    Optional<Profile> findByUid(String uid);

    Optional<Profile> findByEmail(String email);

    Optional<Profile> findByPhone(String phone);

    Optional<Profile> findById(Long id);

    List<Profile> findAllById(List<Long> ids);

    Page<Profile> findAll(Pageable pageable);

    List<Profile> findAllForIndexing();

    boolean uidExists(String uid);
}
