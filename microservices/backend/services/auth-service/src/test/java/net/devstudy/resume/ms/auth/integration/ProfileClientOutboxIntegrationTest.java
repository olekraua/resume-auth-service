package net.devstudy.resume.ms.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import net.devstudy.resume.auth.api.service.ProfileAccountService;
import net.devstudy.resume.auth.api.service.RestoreAccessService;
import net.devstudy.resume.auth.internal.client.HttpProfileInternalClient;
import net.devstudy.resume.auth.internal.client.ProfileInternalClient;
import net.devstudy.resume.auth.internal.entity.AuthOutboxEvent;
import net.devstudy.resume.auth.internal.entity.AuthOutboxEventType;
import net.devstudy.resume.auth.internal.entity.AuthOutboxStatus;
import net.devstudy.resume.auth.internal.entity.ProfileRestore;
import net.devstudy.resume.auth.internal.outbox.AuthOutboxListener;
import net.devstudy.resume.auth.internal.outbox.AuthOutboxWriter;
import net.devstudy.resume.auth.internal.repository.storage.AuthOutboxRepository;
import net.devstudy.resume.auth.internal.repository.storage.ProfileRestoreRepository;
import net.devstudy.resume.auth.internal.service.impl.RestoreAccessServiceImpl;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfilePasswordUpdateRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileUidUpdateRequest;
import net.devstudy.resume.shared.component.DataBuilder;

@SpringBootTest(classes = ProfileClientOutboxIntegrationTest.TestApp.class)
@Testcontainers
@Tag("integration")
class ProfileClientOutboxIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resume_auth")
            .withUsername("resume")
            .withPassword("resume");

    private static final AtomicInteger LOOKUP_CALLS = new AtomicInteger();
    private static final AtomicReference<String> LAST_AUTHORIZATION = new AtomicReference<>();
    private static final AtomicReference<String> LAST_LOOKUP_BODY = new AtomicReference<>();

    private static HttpServer profileApiServer;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/auth_nodup");
        registry.add("app.outbox.enabled", () -> "true");
        registry.add("app.services.profile.base-url", ProfileClientOutboxIntegrationTest::profileBaseUrl);
    }

    @Autowired
    private RestoreAccessService restoreAccessService;

    @Autowired
    private ProfileRestoreRepository profileRestoreRepository;

    @Autowired
    private AuthOutboxRepository authOutboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startProfileApiServer() {
        ensureProfileServerStarted();
    }

    @AfterAll
    static void stopProfileApiServer() {
        if (profileApiServer != null) {
            profileApiServer.stop(0);
            profileApiServer = null;
        }
    }

    @BeforeEach
    void resetState() {
        LOOKUP_CALLS.set(0);
        LAST_AUTHORIZATION.set(null);
        LAST_LOOKUP_BODY.set(null);
        authOutboxRepository.deleteAll();
        profileRestoreRepository.deleteAll();
    }

    @Test
    void shouldCallProfileInternalLookupAndWriteRestoreMailToOutbox() throws Exception {
        String link = restoreAccessService.requestRestore("john@example.com", "https://app.local");

        assertThat(LOOKUP_CALLS.get()).isEqualTo(1);
        assertThat(LAST_AUTHORIZATION.get()).isEqualTo("Bearer test-service-token");
        assertThat(LAST_LOOKUP_BODY.get()).contains("\"identifier\":\"john@example.com\"");

        Optional<ProfileRestore> profileRestore = profileRestoreRepository.findByProfileId(101L);
        assertThat(profileRestore).isPresent();
        assertThat(profileRestore.get().getCreated()).isBeforeOrEqualTo(Instant.now());
        assertThat(profileRestore.get().getToken()).hasSize(64).matches("^[0-9a-f]+$");

        String tokenFromLink = extractToken(link);
        assertThat(profileRestore.get().getToken()).isNotEqualTo(tokenFromLink);

        assertThat(authOutboxRepository.findAll()).hasSize(1);
        AuthOutboxEvent outboxEvent = authOutboxRepository.findAll().getFirst();
        assertThat(outboxEvent.getEventType()).isEqualTo(AuthOutboxEventType.RESTORE_ACCESS_MAIL);
        assertThat(outboxEvent.getStatus()).isEqualTo(AuthOutboxStatus.NEW);
        assertThat(outboxEvent.getAttempts()).isZero();

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(outboxEvent.getPayload(), Map.class);
        assertThat(payload.get("email")).isEqualTo("john@example.com");
        assertThat(payload.get("firstName")).isEqualTo("John");
        assertThat(payload.get("link")).isEqualTo(link);
    }

    private static String extractToken(String link) {
        int idx = link.lastIndexOf('/');
        return idx >= 0 ? link.substring(idx + 1) : link;
    }

    private static synchronized String profileBaseUrl() {
        ensureProfileServerStarted();
        return "http://127.0.0.1:" + profileApiServer.getAddress().getPort();
    }

    private static synchronized void ensureProfileServerStarted() {
        if (profileApiServer != null) {
            return;
        }
        try {
            profileApiServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            profileApiServer.createContext("/internal/profiles/lookup",
                    ProfileClientOutboxIntegrationTest::handleLookupRequest);
            profileApiServer.start();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start profile API test server", ex);
        }
    }

    private static void handleLookupRequest(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeResponse(exchange, 405, "");
            return;
        }
        LOOKUP_CALLS.incrementAndGet();
        LAST_AUTHORIZATION.set(exchange.getRequestHeaders().getFirst("Authorization"));
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        LAST_LOOKUP_BODY.set(body);
        if (body.contains("\"identifier\":\"john@example.com\"")) {
            writeJsonResponse(exchange, 200, """
                    {
                      "id": 101,
                      "uid": "john",
                      "email": "john@example.com",
                      "phone": "+123456789",
                      "firstName": "John",
                      "lastName": "Doe"
                    }
                    """);
            return;
        }
        writeResponse(exchange, 404, "");
    }

    private static void writeJsonResponse(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        writeResponse(exchange, status, body);
    }

    private static void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            ProfileRestore.class,
            AuthOutboxEvent.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            ProfileRestoreRepository.class,
            AuthOutboxRepository.class
    })
    @Import({
            RestoreAccessServiceImpl.class,
            AuthOutboxWriter.class,
            AuthOutboxListener.class
    })
    static class TestApp {

        @Bean
        RestClient profileInternalRestClient(@Autowired org.springframework.core.env.Environment environment) {
            String baseUrl = environment.getRequiredProperty("app.services.profile.base-url");
            return RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestInterceptor((request, body, execution) -> {
                        request.getHeaders().setBearerAuth("test-service-token");
                        return execution.execute(request, body);
                    })
                    .build();
        }

        @Bean
        ProfileInternalClient profileInternalClient(RestClient profileInternalRestClient) {
            return new HttpProfileInternalClient(profileInternalRestClient);
        }

        @Bean
        DataBuilder dataBuilder() {
            return new DataBuilder() {
                @Override
                public String buildProfileUid(String firstName, String lastName) {
                    return "uid";
                }

                @Override
                public String buildRestoreAccessLink(String appHost, String token) {
                    return appHost + "/restore/" + token;
                }

                @Override
                public String rebuildUidWithRandomSuffix(String baseUid, String alphabet, int letterCount) {
                    return baseUid + "-rnd";
                }

                @Override
                public String buildCertificateName(String fileName) {
                    return fileName == null ? "" : fileName;
                }
            };
        }

        @Bean
        ProfileAccountService profileAccountService() {
            return new ProfileAccountService() {
                @Override
                public ProfileAuthResponse register(ProfileRegistrationRequest request) {
                    throw new UnsupportedOperationException("Not needed for this integration test");
                }

                @Override
                public ProfileAuthResponse loadForAuth(String uid) {
                    throw new UnsupportedOperationException("Not needed for this integration test");
                }

                @Override
                public void updatePassword(Long profileId, ProfilePasswordUpdateRequest request) {
                    throw new UnsupportedOperationException("Not needed for this integration test");
                }

                @Override
                public void updateUid(Long profileId, ProfileUidUpdateRequest request) {
                    throw new UnsupportedOperationException("Not needed for this integration test");
                }

                @Override
                public void removeProfile(Long profileId) {
                    throw new UnsupportedOperationException("Not needed for this integration test");
                }
            };
        }
    }
}
