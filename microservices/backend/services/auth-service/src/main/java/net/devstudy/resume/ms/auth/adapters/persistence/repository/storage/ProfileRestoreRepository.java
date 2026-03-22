package net.devstudy.resume.ms.auth.adapters.persistence.repository.storage;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.devstudy.resume.ms.auth.domain.entity.ProfileRestore;

public interface ProfileRestoreRepository extends JpaRepository<ProfileRestore, Long> {

    Optional<ProfileRestore> findByToken(String token);

    Optional<ProfileRestore> findByProfileId(Long profileId);

    void deleteByProfileId(Long profileId);
}
