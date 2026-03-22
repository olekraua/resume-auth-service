package net.devstudy.resume.messaging.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import net.devstudy.resume.messaging.api.model.Conversation;
import net.devstudy.resume.messaging.internal.repository.storage.ConversationRepository;

@Configuration
@EntityScan(basePackageClasses = Conversation.class)
@EnableJpaRepositories(basePackageClasses = ConversationRepository.class)
public class MessagingJpaConfig {
}
