package net.devstudy.resume.staticdata.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import net.devstudy.resume.staticdata.api.model.SkillCategory;
import net.devstudy.resume.staticdata.internal.repository.storage.SkillCategoryRepository;

@Configuration
@ConditionalOnProperty(name = "app.services.staticdata.mode", havingValue = "local", matchIfMissing = true)
@EntityScan(basePackageClasses = SkillCategory.class)
@EnableJpaRepositories(basePackageClasses = SkillCategoryRepository.class)
public class StaticDataJpaConfig {
}
