package net.devstudy.resume.profile.internal.repository.storage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.devstudy.resume.profile.api.model.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByProfileIdOrderByFinishDateDesc(Long profileId);

    void deleteByProfileId(Long profileId);
}
