package net.devstudy.resume.web.security;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.security.session.enabled", havingValue = "true", matchIfMissing = true)
public class RememberMeSupport {

    private static final String DEFAULT_REMEMBER_ME_KEY = "resume-remember-me-key";
    private static final String DEFAULT_REMEMBER_ME_PARAMETER = "remember-me";
    private static final String DEFAULT_REMEMBER_ME_COOKIE_NAME = "remember-me";
    private static final Duration DEFAULT_REMEMBER_ME_TTL = Duration.ofDays(14);

    private final ObjectProvider<PersistentTokenRepository> rememberMeServiceProvider;
    private final UserDetailsService userDetailsService;

    @Value("${app.security.remember-me.key:resume-remember-me-key}")
    private String rememberMeKey;

    @Value("${app.security.remember-me.token-ttl:PT336H}")
    private Duration rememberMeTtl;

    @Value("${app.security.remember-me.parameter:remember-me}")
    private String rememberMeParameter;

    @Value("${app.security.remember-me.cookie-name:remember-me}")
    private String rememberMeCookieName;

    private volatile PersistentTokenBasedRememberMeServices rememberMeServices;

    public RememberMeSupport(ObjectProvider<PersistentTokenRepository> rememberMeServiceProvider,
            UserDetailsService userDetailsService) {
        this.rememberMeServiceProvider = rememberMeServiceProvider;
        this.userDetailsService = userDetailsService;
    }

    public void loginSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication, boolean rememberMe) {
        PersistentTokenBasedRememberMeServices services = getRememberMeServices();
        if (services == null) {
            return;
        }
        if (rememberMe) {
            services.loginSuccess(request, response, authentication);
        } else {
            services.logout(request, response, authentication);
        }
    }

    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        PersistentTokenBasedRememberMeServices services = getRememberMeServices();
        if (services != null) {
            services.logout(request, response, authentication);
        }
    }

    private PersistentTokenBasedRememberMeServices getRememberMeServices() {
        if (rememberMeServices != null) {
            return rememberMeServices;
        }
        PersistentTokenRepository repository = rememberMeServiceProvider.getIfAvailable();
        if (repository == null) {
            return null;
        }
        PersistentTokenBasedRememberMeServices services = new PersistentTokenBasedRememberMeServices(
                normalizeRememberMeValue(rememberMeKey, DEFAULT_REMEMBER_ME_KEY),
                userDetailsService,
                repository
        );
        services.setParameter(normalizeRememberMeValue(rememberMeParameter, DEFAULT_REMEMBER_ME_PARAMETER));
        services.setCookieName(normalizeRememberMeValue(rememberMeCookieName, DEFAULT_REMEMBER_ME_COOKIE_NAME));
        services.setTokenValiditySeconds(toRememberMeSeconds(rememberMeTtl));
        rememberMeServices = services;
        return services;
    }

    private int toRememberMeSeconds(Duration ttl) {
        Duration normalized = ttl;
        if (normalized == null || normalized.isNegative() || normalized.isZero()) {
            normalized = DEFAULT_REMEMBER_ME_TTL;
        }
        long seconds = normalized.getSeconds();
        if (seconds > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) seconds;
    }

    private String normalizeRememberMeValue(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }
}
