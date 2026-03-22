package net.devstudy.resume.ms.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import net.devstudy.resume.auth.api.config.AuthJpaConfig;
import net.devstudy.resume.auth.internal.client.HttpProfileInternalClient;
import net.devstudy.resume.auth.internal.client.ProfileInternalClientConfig;
import net.devstudy.resume.auth.internal.component.impl.AccessDeniedHandlerImpl;
import net.devstudy.resume.auth.internal.config.SecurityConfig;
import net.devstudy.resume.auth.internal.event.ProfilePasswordChangedListener;
import net.devstudy.resume.auth.internal.outbox.AuthOutboxListener;
import net.devstudy.resume.auth.internal.outbox.AuthOutboxWriter;
import net.devstudy.resume.auth.internal.security.LoginProtectionService;
import net.devstudy.resume.auth.internal.security.SecurityContextCurrentProfileProvider;
import net.devstudy.resume.auth.internal.service.impl.CurrentProfileDetailsService;
import net.devstudy.resume.auth.internal.service.impl.ProfileAccountServiceImpl;
import net.devstudy.resume.auth.internal.service.impl.RememberMeService;
import net.devstudy.resume.auth.internal.service.impl.RestoreAccessServiceImpl;
import net.devstudy.resume.auth.internal.service.impl.UidSuggestionServiceImpl;

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
