package net.devstudy.resume.profile.internal.repository.storage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.devstudy.resume.profile.api.model.Practic;

public interface PracticRepository extends JpaRepository<Practic, Long> {
    List<Practic> findByProfileIdOrderByFinishDateDesc(Long profileId);

    void deleteByProfileId(Long profileId);
}
