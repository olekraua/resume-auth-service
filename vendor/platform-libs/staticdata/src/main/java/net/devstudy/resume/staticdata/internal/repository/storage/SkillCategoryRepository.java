package net.devstudy.resume.staticdata.internal.repository.storage;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import net.devstudy.resume.staticdata.api.model.SkillCategory;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {

    @Override
    @NonNull
    List<SkillCategory> findAll(@NonNull Sort sort);
}
