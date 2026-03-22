package net.devstudy.resume.ms.auth.config.persistence;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import net.devstudy.resume.ms.auth.domain.entity.RememberMeToken;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.RememberMeTokenRepository;

@Configuration
@EntityScan(basePackageClasses = RememberMeToken.class)
@EnableJpaRepositories(basePackageClasses = RememberMeTokenRepository.class)
public class AuthJpaConfig {
}
