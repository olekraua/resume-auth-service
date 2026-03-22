package net.devstudy.resume.profile.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.internal.repository.storage.ProfileRepository;

@Configuration
@EntityScan(basePackageClasses = Profile.class)
@EnableJpaRepositories(basePackageClasses = ProfileRepository.class)
public class ProfileJpaConfig {
}
