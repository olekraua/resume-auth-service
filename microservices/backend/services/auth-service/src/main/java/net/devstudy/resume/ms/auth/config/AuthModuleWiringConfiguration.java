package net.devstudy.resume.ms.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import net.devstudy.resume.ms.auth.config.persistence.AuthJpaConfig;
import net.devstudy.resume.ms.auth.adapters.profile.client.HttpProfileInternalClient;
import net.devstudy.resume.ms.auth.adapters.profile.client.ProfileInternalClientConfig;
import net.devstudy.resume.ms.auth.adapters.web.component.impl.AccessDeniedHandlerImpl;
import net.devstudy.resume.ms.auth.config.security.SecurityConfig;
import net.devstudy.resume.ms.auth.adapters.messaging.event.ProfilePasswordChangedListener;
import net.devstudy.resume.ms.auth.adapters.outbox.AuthOutboxListener;
import net.devstudy.resume.ms.auth.adapters.outbox.AuthOutboxWriter;
import net.devstudy.resume.ms.auth.application.security.LoginProtectionService;
import net.devstudy.resume.ms.auth.application.security.SecurityContextCurrentProfileProvider;
import net.devstudy.resume.ms.auth.application.service.impl.CurrentProfileDetailsService;
import net.devstudy.resume.ms.auth.application.service.impl.ProfileAccountServiceImpl;
import net.devstudy.resume.ms.auth.application.service.impl.RememberMeService;
import net.devstudy.resume.ms.auth.application.service.impl.RestoreAccessServiceImpl;
import net.devstudy.resume.ms.auth.application.service.impl.UidSuggestionServiceImpl;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTestContextBootstrapper",
        havingValue = "false",
        matchIfMissing = true)
@Import({
        AuthJpaConfig.class,
        ProfileInternalClientConfig.class,
        SecurityConfig.class,
        HttpProfileInternalClient.class,
        AccessDeniedHandlerImpl.class,
        ProfilePasswordChangedListener.class,
        AuthOutboxWriter.class,
        AuthOutboxListener.class,
        LoginProtectionService.class,
        SecurityContextCurrentProfileProvider.class,
        CurrentProfileDetailsService.class,
        ProfileAccountServiceImpl.class,
        RememberMeService.class,
        RestoreAccessServiceImpl.class,
        UidSuggestionServiceImpl.class
})
public class AuthModuleWiringConfiguration {
}
