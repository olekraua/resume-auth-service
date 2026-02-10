package net.devstudy.resume.auth.internal.config;

import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.util.StringUtils;

import net.devstudy.resume.auth.internal.service.impl.RememberMeService;

@Configuration
@ConditionalOnProperty(name = "app.security.session.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    private static final String DEFAULT_REMEMBER_ME_KEY = "resume-remember-me-key";
    private static final String DEFAULT_REMEMBER_ME_PARAMETER = "remember-me";
    private static final String DEFAULT_REMEMBER_ME_COOKIE_NAME = "remember-me";
    private static final Duration DEFAULT_REMEMBER_ME_TTL = Duration.ofDays(14);

    @Value("${app.security.remember-me.key:resume-remember-me-key}")
    private String rememberMeKey;

    @Value("${app.security.remember-me.token-ttl:PT336H}")
    private Duration rememberMeTtl;

    @Value("${app.security.remember-me.parameter:remember-me}")
    private String rememberMeParameter;

    @Value("${app.security.remember-me.cookie-name:remember-me}")
    private String rememberMeCookieName;

    @Value("${app.security.csrf.cookie.same-site:Lax}")
    private String csrfCookieSameSite;

    @Value("${app.security.csrf.cookie.secure:false}")
    private boolean csrfCookieSecure;

    @Value("${app.security.csrf.ignore-auth-endpoints:false}")
    private boolean ignoreAuthEndpointsCsrf;

    @Value("${app.security.csrf.ignore-api:false}")
    private boolean ignoreApiCsrf;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            UserDetailsService userDetailsService,
            AccessDeniedHandler accessDeniedHandler,
            ObjectProvider<RememberMeService> rememberMeServiceProvider)
            throws Exception {
        http
                .cors(withDefaults())
                .csrf(csrf -> {
                        csrf.csrfTokenRepository(buildCsrfTokenRepository())
                                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler());
                        if (ignoreAuthEndpointsCsrf) {
                            csrf.ignoringRequestMatchers(
                                    PathPatternRequestMatcher.withDefaults()
                                            .matcher(HttpMethod.POST, "/api/auth/login"),
                                    PathPatternRequestMatcher.withDefaults()
                                            .matcher(HttpMethod.POST, "/api/auth/register"),
                                    PathPatternRequestMatcher.withDefaults()
                                            .matcher(HttpMethod.POST, "/api/auth/restore"),
                                    PathPatternRequestMatcher.withDefaults()
                                            .matcher(HttpMethod.POST, "/api/auth/restore/*")
                            );
                        }
                        if (ignoreApiCsrf) {
                            csrf.ignoringRequestMatchers(
                                    PathPatternRequestMatcher.withDefaults().matcher("/api/**")
                            );
                        }
                })
                .authorizeHttpRequests(auth -> {
                        auth
                                .requestMatchers("/api/auth/**", "/api/csrf").permitAll()
                                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                                // public GET API for frontend
                                .requestMatchers(HttpMethod.GET,
                                        "/api/me",
                                        "/api/profiles",
                                        "/api/profiles/*",
                                        "/api/search",
                                        "/api/suggest",
                                        "/api/static-data")
                                .permitAll()
                                .requestMatchers("/api/**").authenticated()
                                .requestMatchers("/actuator/**").authenticated();
                        auth.anyRequest().denyAll();
                })
                .userDetailsService(userDetailsService)
                .exceptionHandling(ex -> {
                        ex.accessDeniedHandler(accessDeniedHandler);
                        ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
                });

        RememberMeService rememberMeService = rememberMeServiceProvider.getIfAvailable();
        if (rememberMeService != null) {
            http.rememberMe(remember -> remember
                    .tokenRepository(rememberMeService)
                    .key(normalizeRememberMeValue(rememberMeKey, DEFAULT_REMEMBER_ME_KEY))
                    .rememberMeParameter(normalizeRememberMeValue(rememberMeParameter,
                            DEFAULT_REMEMBER_ME_PARAMETER))
                    .rememberMeCookieName(normalizeRememberMeValue(rememberMeCookieName,
                            DEFAULT_REMEMBER_ME_COOKIE_NAME))
                    .tokenValiditySeconds(toRememberMeSeconds(rememberMeTtl))
                    .userDetailsService(userDetailsService));
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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

    private CookieCsrfTokenRepository buildCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        String sameSite = normalizeSameSite(csrfCookieSameSite);
        repository.setCookieCustomizer(builder -> {
            if (StringUtils.hasText(sameSite)) {
                builder.sameSite(sameSite);
            }
            boolean secure = csrfCookieSecure || "None".equalsIgnoreCase(sameSite);
            builder.secure(secure);
        });
        return repository;
    }

    private String normalizeSameSite(String value) {
        if (!StringUtils.hasText(value)) {
            return "Lax";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "lax" -> "Lax";
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> "Lax";
        };
    }
}
