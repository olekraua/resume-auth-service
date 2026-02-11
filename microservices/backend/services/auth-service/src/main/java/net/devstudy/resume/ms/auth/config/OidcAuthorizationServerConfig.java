package net.devstudy.resume.ms.auth.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import net.devstudy.resume.auth.api.model.CurrentProfile;
import net.devstudy.resume.auth.internal.security.LoginLockedException;
import net.devstudy.resume.auth.internal.security.LoginProtectionService;
import net.devstudy.resume.ms.auth.security.PersistentJwtSigningKeyStore;
import net.devstudy.resume.web.security.CurrentProfileJwtConverter;
import net.devstudy.resume.web.security.JwtAuthenticationFailureEntryPoint;

@Configuration
@ConditionalOnProperty(name = "app.security.oidc.enabled", havingValue = "true")
public class OidcAuthorizationServerConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
            RequestCache requestCache) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        authorizationServerConfigurer.oidc(withDefaults());

        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        http.securityMatcher(endpointsMatcher)
                .cors(withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .requestCache(cache -> cache.requestCache(requestCache))
                .apply(authorizationServerConfigurer);

        http.authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated());

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
        );
        return http.build();
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
            CurrentProfileJwtConverter currentProfileJwtConverter,
            JwtAuthenticationFailureEntryPoint jwtAuthenticationFailureEntryPoint,
            RequestCache requestCache,
            AuthenticationSuccessHandler authenticationSuccessHandler,
            AuthenticationFailureHandler authenticationFailureHandler) throws Exception {
        http
                .cors(withDefaults())
                .requestCache(cache -> cache.requestCache(requestCache))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/me").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/uid-hint").permitAll()
                        .requestMatchers("/error", "/favicon.ico", "/default-ui.css").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/restore").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/restore/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/restore/*").permitAll()
                        .requestMatchers("/api/csrf").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(jwtAuthenticationFailureEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(currentProfileJwtConverter)))
                .formLogin(form -> form
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler));
        return http.build();
    }

    @Bean
    public RequestCache requestCache() {
        return new HttpSessionRequestCache();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(RequestCache requestCache,
            LoginProtectionService loginProtectionService) {
        SavedRequestAwareAuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
        delegate.setRequestCache(requestCache);
        return (request, response, authentication) -> {
            loginProtectionService.onAuthenticationSuccess(authentication.getName());
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            String rewrittenTargetUrl = rewriteSavedErrorRedirect(savedRequest);
            if (rewrittenTargetUrl != null) {
                requestCache.removeRequest(request, response);
                response.sendRedirect(rewrittenTargetUrl);
                return;
            }
            delegate.onAuthenticationSuccess(request, response, authentication);
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler(LoginProtectionService loginProtectionService) {
        SimpleUrlAuthenticationFailureHandler delegate = new SimpleUrlAuthenticationFailureHandler("/login?error");
        return (request, response, exception) -> {
            LoginLockedException loginLockedException = extractLoginLockedException(exception);
            boolean locked = loginLockedException != null || exception instanceof LockedException;
            if (!locked) {
                String username = request.getParameter(
                        UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY);
                loginProtectionService.onAuthenticationFailure(username);
            }
            if (loginLockedException != null) {
                response.setHeader("Retry-After", Long.toString(loginLockedException.getRetryAfterSeconds()));
            }
            delegate.onAuthenticationFailure(request, response, exception);
        };
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate,
            @Value("${app.security.oidc.client-id:resume-spa}") String clientId,
            @Value("${app.security.oidc.redirect-uri:https://localhost:4200/auth/callback}") String redirectUri,
            @Value("${app.security.oidc.post-logout-redirect-uri:https://localhost:4200/}") String postLogoutRedirectUri,
            @Value("${app.security.oidc.access-token-ttl:PT10M}") Duration accessTokenTtl,
            @Value("${app.security.oidc.internal-client.client-id:resume-profile-internal}") String internalClientId,
            @Value("${app.security.oidc.internal-client.client-secret:}") String internalClientSecret,
            @Value("${app.security.oidc.internal-client.scope:internal.profile}") String internalClientScope,
            @Value("${app.security.oidc.internal-client.access-token-ttl:PT2M}") Duration internalClientAccessTokenTtl,
            PasswordEncoder passwordEncoder) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);
        RegisteredClient existing = repository.findByClientId(clientId);
        if (!isPublicSpaClientUpToDate(existing, redirectUri, postLogoutRedirectUri, accessTokenTtl)) {
            String id = existing != null ? existing.getId() : UUID.randomUUID().toString();
            repository.save(buildPublicSpaClient(id, clientId, redirectUri, postLogoutRedirectUri, accessTokenTtl));
        }
        if (!StringUtils.hasText(internalClientSecret)) {
            throw new IllegalStateException("app.security.oidc.internal-client.client-secret must be configured");
        }
        RegisteredClient existingInternalClient = repository.findByClientId(internalClientId);
        if (!isServiceClientUpToDate(existingInternalClient,
                internalClientScope,
                internalClientAccessTokenTtl,
                internalClientSecret,
                passwordEncoder)) {
            String id = existingInternalClient != null
                    ? existingInternalClient.getId()
                    : UUID.randomUUID().toString();
            repository.save(buildServiceClient(id,
                    internalClientId,
                    internalClientSecret,
                    internalClientScope,
                    internalClientAccessTokenTtl,
                    passwordEncoder));
        }
        return repository;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        JdbcOAuth2AuthorizationService authorizationService =
                new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        SimpleModule currentProfileModule = new SimpleModule();
        currentProfileModule.addDeserializer(CurrentProfile.class, new CurrentProfileDeserializer());
        objectMapper.registerModule(currentProfileModule);
        objectMapper.addMixIn(CurrentProfile.class, CurrentProfileMixin.class);

        JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper authorizationRowMapper =
                new JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper(registeredClientRepository);
        authorizationRowMapper.setObjectMapper(objectMapper);

        JdbcOAuth2AuthorizationService.OAuth2AuthorizationParametersMapper authorizationParametersMapper =
                new JdbcOAuth2AuthorizationService.OAuth2AuthorizationParametersMapper();
        authorizationParametersMapper.setObjectMapper(objectMapper);

        authorizationService.setAuthorizationRowMapper(authorizationRowMapper);
        authorizationService.setAuthorizationParametersMapper(authorizationParametersMapper);
        return authorizationService;
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(PersistentJwtSigningKeyStore persistentJwtSigningKeyStore) {
        return (jwkSelector, securityContext) ->
                jwkSelector.select(persistentJwtSigningKeyStore.loadOrCreateSigningJwkSet());
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);
        jwtEncoder.setJwkSelector(OidcAuthorizationServerConfig::selectPreferredSigningKey);
        return jwtEncoder;
    }

    @Bean
    public SmartInitializingSingleton oidcSigningKeyStoreFailFastInitializer(
            PersistentJwtSigningKeyStore persistentJwtSigningKeyStore) {
        return persistentJwtSigningKeyStore::verifyStoreAvailabilityOrFailFast;
    }

    @Bean
    public SmartInitializingSingleton oidcTokenLifetimeWindowGuard(
            @Value("${app.security.oidc.access-token-ttl:PT10M}") Duration accessTokenTtl,
            @Value("${app.security.oidc.signing-key.token-ttl:${app.security.oidc.access-token-ttl:PT10M}}")
            Duration signingKeyTokenTtl,
            @Value("${app.security.oidc.signing-key.clock-skew:PT1M}") Duration signingKeyClockSkew) {
        return () -> validateTokenLifetimeWindow(accessTokenTtl, signingKeyTokenTtl, signingKeyClockSkew);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @Value("${app.security.oidc.issuer:https://localhost:8443}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (context.getPrincipal() != null
                    && context.getPrincipal().getPrincipal() instanceof CurrentProfile profile) {
                if (profile.getId() != null) {
                    context.getClaims().claim("profile_id", profile.getId().toString());
                }
                context.getClaims().claim("uid", profile.getUsername());
                context.getClaims().claim("name", profile.getFullName());
            }
        };
    }

    private RegisteredClient buildPublicSpaClient(String id, String clientId, String redirectUri,
            String postLogoutRedirectUri,
            Duration accessTokenTtl) {
        return RegisteredClient.withId(id)
                .clientId(clientId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirectUri)
                .postLogoutRedirectUri(postLogoutRedirectUri)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("offline_access")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(accessTokenTtl)
                        .reuseRefreshTokens(false)
                        .build())
                .build();
    }

    private RegisteredClient buildServiceClient(String id,
            String clientId,
            String clientSecret,
            String scope,
            Duration accessTokenTtl,
            PasswordEncoder passwordEncoder) {
        return RegisteredClient.withId(id)
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(scope)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(accessTokenTtl)
                        .build())
                .build();
    }

    private boolean isPublicSpaClientUpToDate(RegisteredClient existing,
            String redirectUri,
            String postLogoutRedirectUri,
            Duration accessTokenTtl) {
        if (existing == null) {
            return false;
        }
        TokenSettings tokenSettings = existing.getTokenSettings();
        return hasPublicSpaAuthenticationAndGrantTypes(existing)
                && hasPublicSpaRedirectUris(existing, redirectUri, postLogoutRedirectUri)
                && hasPublicSpaScopes(existing)
                && hasPublicSpaClientSettings(existing)
                && hasPublicSpaTokenSettings(tokenSettings, accessTokenTtl);
    }

    private boolean hasPublicSpaAuthenticationAndGrantTypes(RegisteredClient existing) {
        return existing.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)
                && existing.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE)
                && existing.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN);
    }

    private boolean hasPublicSpaRedirectUris(RegisteredClient existing,
            String redirectUri,
            String postLogoutRedirectUri) {
        return existing.getRedirectUris().contains(redirectUri)
                && existing.getPostLogoutRedirectUris().contains(postLogoutRedirectUri);
    }

    private boolean hasPublicSpaScopes(RegisteredClient existing) {
        return existing.getScopes().contains(OidcScopes.OPENID)
                && existing.getScopes().contains(OidcScopes.PROFILE)
                && existing.getScopes().contains("offline_access");
    }

    private boolean hasPublicSpaClientSettings(RegisteredClient existing) {
        return existing.getClientSettings().isRequireProofKey()
                && !existing.getClientSettings().isRequireAuthorizationConsent();
    }

    private boolean hasPublicSpaTokenSettings(TokenSettings tokenSettings, Duration accessTokenTtl) {
        return tokenSettings != null
                && accessTokenTtl.equals(tokenSettings.getAccessTokenTimeToLive())
                && !tokenSettings.isReuseRefreshTokens();
    }

    private boolean isServiceClientUpToDate(RegisteredClient existing,
            String scope,
            Duration accessTokenTtl,
            String rawClientSecret,
            PasswordEncoder passwordEncoder) {
        if (existing == null || !StringUtils.hasText(rawClientSecret)) {
            return false;
        }
        TokenSettings tokenSettings = existing.getTokenSettings();
        return existing.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                && existing.getAuthorizationGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS)
                && existing.getScopes().contains(scope)
                && StringUtils.hasText(existing.getClientSecret())
                && passwordEncoder.matches(rawClientSecret, existing.getClientSecret())
                && tokenSettings != null
                && accessTokenTtl.equals(tokenSettings.getAccessTokenTimeToLive());
    }

    private void validateTokenLifetimeWindow(Duration accessTokenTtl,
            Duration signingKeyTokenTtl,
            Duration signingKeyClockSkew) {
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalStateException("app.security.oidc.access-token-ttl must be positive");
        }
        if (signingKeyTokenTtl == null || signingKeyTokenTtl.isNegative() || signingKeyTokenTtl.isZero()) {
            throw new IllegalStateException("app.security.oidc.signing-key.token-ttl must be positive");
        }
        if (signingKeyClockSkew != null && signingKeyClockSkew.isNegative()) {
            throw new IllegalStateException("app.security.oidc.signing-key.clock-skew must be non-negative");
        }
        if (accessTokenTtl.compareTo(signingKeyTokenTtl) > 0) {
            throw new IllegalStateException(
                    "access-token-ttl must be <= signing-key.token-ttl to keep old kid available for active tokens");
        }
    }

    private static JWK selectPreferredSigningKey(List<JWK> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No matching JWK found for JWT signing");
        }
        for (JWK candidate : candidates) {
            if (candidate != null && candidate.isPrivate()) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private String rewriteSavedErrorRedirect(SavedRequest savedRequest) {
        if (savedRequest == null) {
            return null;
        }
        UriComponents savedRedirect = UriComponentsBuilder.fromUriString(savedRequest.getRedirectUrl()).build(true);
        if (!"/error".equals(savedRedirect.getPath())) {
            return null;
        }
        if (!savedRedirect.getQueryParams().containsKey("client_id")
                || !"code".equals(savedRedirect.getQueryParams().getFirst("response_type"))) {
            return null;
        }
        UriComponentsBuilder redirectToAuthorize = UriComponentsBuilder.newInstance()
                .scheme(savedRedirect.getScheme())
                .host(savedRedirect.getHost())
                .port(savedRedirect.getPort())
                .path("/oauth2/authorize");
        savedRedirect.getQueryParams().forEach((key, values) -> {
            if (!"continue".equals(key)) {
                values.forEach(value -> redirectToAuthorize.queryParam(key, value));
            }
        });
        return redirectToAuthorize.build(true).toUriString();
    }

    private LoginLockedException extractLoginLockedException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof LoginLockedException loginLockedException) {
                return loginLockedException;
            }
            current = current.getCause();
        }
        return null;
    }

    @JsonDeserialize(using = CurrentProfileDeserializer.class)
    private abstract static class CurrentProfileMixin {

        @JsonCreator
        CurrentProfileMixin(@JsonProperty("id") Long id,
                @JsonProperty("username") String uid,
                @JsonProperty("fullName") String fullName) {
        }
    }

    private static final class CurrentProfileDeserializer extends JsonDeserializer<CurrentProfile> {

        @Override
        public CurrentProfile deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            return new CurrentProfile(
                    getNullableLong(node, "id"),
                    getNullableText(node, "username"),
                    getNullableText(node, "fullName"));
        }

        private Long getNullableLong(JsonNode node, String field) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.longValue();
        }

        private String getNullableText(JsonNode node, String field) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.asText();
        }
    }
}
