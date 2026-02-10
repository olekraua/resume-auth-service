package net.devstudy.resume.profile.internal.repository.storage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.devstudy.resume.profile.api.model.Language;

public interface LanguageRepository extends JpaRepository<Language, Long> {
    List<Language> findByProfileId(Long profileId);

    void deleteByProfileId(Long profileId);
}
