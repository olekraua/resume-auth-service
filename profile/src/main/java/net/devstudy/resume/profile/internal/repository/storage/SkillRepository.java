package net.devstudy.resume.profile.internal.repository.storage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.devstudy.resume.profile.api.model.Skill;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByProfileIdOrderByCategoryAsc(Long profileId);

    List<Skill> findByProfileIdOrderByIdAsc(Long profileId);

    void deleteByProfileId(Long profileId);
}
