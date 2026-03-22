package net.devstudy.resume.auth.internal.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import net.devstudy.resume.shared.middleware.http.PlatformClientHttpRequestFactoryProvider;
import net.devstudy.resume.shared.middleware.http.PlatformRestClientCustomizer;

@Configuration
@ConditionalOnProperty(name = "app.services.profile.mode", havingValue = "remote")
public class ProfileInternalClientConfig {

    private static final Duration TOKEN_EXPIRY_SKEW = Duration.ofSeconds(5);

    @Bean
    public RestClient profileInternalRestClient(
            @Value("${app.services.profile.base-url}") String baseUrl,
            @Value("${app.services.profile.service-jwt.token-uri}") String tokenUri,
            @Value("${app.services.profile.service-jwt.client-id}") String clientId,
            @Value("${app.services.profile.service-jwt.client-secret}") String clientSecret,
            @Value("${app.services.profile.service-jwt.scope:internal.profile}") String scope,
            @Value("${app.services.profile.mtls.enabled:true}") boolean mtlsEnabled,
            @Value("${app.services.profile.mtls.key-store-path:}") String keyStorePath,
            @Value("${app.services.profile.mtls.key-store-password:}") String keyStorePassword,
            @Value("${app.services.profile.mtls.key-store-type:PKCS12}") String keyStoreType,
            @Value("${app.services.profile.mtls.trust-store-path:}") String trustStorePath,
            @Value("${app.services.profile.mtls.trust-store-password:}") String trustStorePassword,
            @Value("${app.services.profile.mtls.trust-store-type:PKCS12}") String trustStoreType,
            PlatformRestClientCustomizer platformRestClientCustomizer,
            PlatformClientHttpRequestFactoryProvider requestFactoryProvider) {
        RestClient tokenRestClient = platformRestClientCustomizer
                .apply(RestClient.builder(), "profile-service-token")
                .build();
        ServiceJwtTokenProvider tokenProvider =
                new ServiceJwtTokenProvider(tokenRestClient, tokenUri, clientId, clientSecret, scope);
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenProvider.getAccessToken());
                    return execution.execute(request, body);
                });
        platformRestClientCustomizer.apply(builder, "profile-service-internal");
        if (mtlsEnabled) {
            builder.requestFactory(buildMtlsRequestFactory(baseUrl,
                    keyStorePath,
                    keyStorePassword,
                    keyStoreType,
                    trustStorePath,
                    trustStorePassword,
                    trustStoreType,
                    requestFactoryProvider));
        }
        return builder.build();
    }

    private ClientHttpRequestFactory buildMtlsRequestFactory(
            String baseUrl,
            String keyStorePath,
            String keyStorePassword,
            String keyStoreType,
            String trustStorePath,
            String trustStorePassword,
            String trustStoreType,
            PlatformClientHttpRequestFactoryProvider requestFactoryProvider) {
        requireText(baseUrl, "app.services.profile.base-url must be configured");
        if (!baseUrl.startsWith("https://")) {
            throw new IllegalStateException("mTLS requires https profile base-url");
        }
        requireText(keyStorePath, "app.services.profile.mtls.key-store-path must be configured");
        requireText(keyStorePassword, "app.services.profile.mtls.key-store-password must be configured");
        requireText(keyStoreType, "app.services.profile.mtls.key-store-type must be configured");
        requireText(trustStorePath, "app.services.profile.mtls.trust-store-path must be configured");
        requireText(trustStorePassword, "app.services.profile.mtls.trust-store-password must be configured");
        requireText(trustStoreType, "app.services.profile.mtls.trust-store-type must be configured");

        SSLContext sslContext = buildSslContext(
                keyStorePath,
                keyStorePassword,
                keyStoreType,
                trustStorePath,
                trustStorePassword,
                trustStoreType);
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .sslContext(sslContext);
        requestFactoryProvider.applyConnectTimeout(clientBuilder);
        return requestFactoryProvider.createFactory(clientBuilder.build());
    }

    private SSLContext buildSslContext(
            String keyStorePath,
            String keyStorePassword,
            String keyStoreType,
            String trustStorePath,
            String trustStorePassword,
            String trustStoreType) {
        try {
            KeyStore keyStore = loadStore(keyStorePath, keyStorePassword, keyStoreType);
            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

            KeyStore trustStore = loadStore(trustStorePath, trustStorePassword, trustStoreType);
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (GeneralSecurityException | IOException ex) {
            throw new IllegalStateException("Failed to initialize mTLS for profile internal client", ex);
        }
    }

    private KeyStore loadStore(String path, String password, String type)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(type);
        Path storePath = Path.of(path);
        try (InputStream inputStream = Files.newInputStream(storePath)) {
            keyStore.load(inputStream, password.toCharArray());
        }
        return keyStore;
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }

    private static final class ServiceJwtTokenProvider {

        private final RestClient tokenRestClient;
        private final String tokenUri;
        private final String clientId;
        private final String clientSecret;
        private final String scope;

        private CachedToken cachedToken;

        private ServiceJwtTokenProvider(RestClient tokenRestClient,
                                        String tokenUri,
                                        String clientId,
                                        String clientSecret,
                                        String scope) {
            this.tokenRestClient = Objects.requireNonNull(tokenRestClient, "tokenRestClient");
            requireText(tokenUri, "app.services.profile.service-jwt.token-uri must be configured");
            requireText(clientId, "app.services.profile.service-jwt.client-id must be configured");
            requireText(clientSecret, "app.services.profile.service-jwt.client-secret must be configured");
            this.tokenUri = tokenUri.trim();
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.scope = scope;
        }

        private synchronized String getAccessToken() {
            Instant now = Instant.now();
            if (cachedToken != null && cachedToken.expiresAt().isAfter(now.plus(TOKEN_EXPIRY_SKEW))) {
                return cachedToken.value();
            }
            TokenResponse response = requestToken();
            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new IllegalStateException("Service JWT token response does not contain access_token");
            }
            long expiresIn = response.expiresIn() == null || response.expiresIn() <= 0 ? 60L : response.expiresIn();
            cachedToken = new CachedToken(response.accessToken(), now.plusSeconds(expiresIn));
            return response.accessToken();
        }

        private TokenResponse requestToken() {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            if (StringUtils.hasText(scope)) {
                form.add("scope", scope.trim());
            }
            return tokenRestClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        }

        private static void requireText(String value, String message) {
            if (!StringUtils.hasText(value)) {
                throw new IllegalStateException(message);
            }
        }
    }

    private record CachedToken(String value, Instant expiresAt) {
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken,
                                 @JsonProperty("expires_in") Long expiresIn) {
    }
}
