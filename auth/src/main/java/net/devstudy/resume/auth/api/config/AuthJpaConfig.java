package net.devstudy.resume.auth.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import net.devstudy.resume.auth.internal.entity.RememberMeToken;
import net.devstudy.resume.auth.internal.repository.storage.RememberMeTokenRepository;

@Configuration
@EntityScan(basePackageClasses = RememberMeToken.class)
@EnableJpaRepositories(basePackageClasses = RememberMeTokenRepository.class)
public class AuthJpaConfig {
}
