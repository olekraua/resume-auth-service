package net.devstudy.resume.ms.auth.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.transaction.Transactional;
import net.devstudy.resume.ms.auth.domain.entity.AuthOutboxEvent;
import net.devstudy.resume.ms.auth.domain.entity.AuthOutboxEventType;
import net.devstudy.resume.ms.auth.domain.entity.AuthOutboxStatus;
import net.devstudy.resume.ms.auth.domain.entity.AuthUser;
import net.devstudy.resume.ms.auth.domain.entity.ProfileRestore;
import net.devstudy.resume.ms.auth.domain.entity.RememberMeToken;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.AuthOutboxRepository;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.AuthUserRepository;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.ProfileRestoreRepository;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.RememberMeTokenRepository;

@SpringBootTest(classes = AuthJpaRepositoriesIntegrationTest.TestApp.class)
@Testcontainers
@Tag("integration")
@Sql(statements = {
        "DELETE FROM oauth2_authorization_consent",
        "DELETE FROM oauth2_authorization",
        "DELETE FROM oauth2_registered_client",
        "DELETE FROM oauth2_signing_jwk_audit",
        "DELETE FROM oauth2_signing_jwk",
        "DELETE FROM oauth2_signing_key",
        "DELETE FROM auth_outbox",
        "DELETE FROM profile_restore",
        "DELETE FROM remember_me_token",
        "DELETE FROM auth_user"
})
class AuthJpaRepositoriesIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resume_auth")
            .withUsername("resume")
            .withPassword("resume");

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/auth_nodup");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private ProfileRestoreRepository profileRestoreRepository;

    @Autowired
    private RememberMeTokenRepository rememberMeTokenRepository;

    @Autowired
    private AuthOutboxRepository authOutboxRepository;

    @BeforeEach
    void cleanUp() {
        authOutboxRepository.deleteAll();
        profileRestoreRepository.deleteAll();
        rememberMeTokenRepository.deleteAll();
        authUserRepository.deleteAll();
    }

    @Test
    void flywayShouldCreateAuthTables() {
        assertThat(tableExists("auth_user")).isTrue();
        assertThat(tableExists("profile_restore")).isTrue();
        assertThat(tableExists("remember_me_token")).isTrue();
        assertThat(tableExists("auth_outbox")).isTrue();
    }

    @Test
    void flywayShouldCreateOidcTables() {
        assertThat(tableExists("oauth2_registered_client")).isTrue();
        assertThat(tableExists("oauth2_authorization")).isTrue();
        assertThat(tableExists("oauth2_authorization_consent")).isTrue();
        assertThat(tableExists("oauth2_signing_key")).isTrue();
        assertThat(tableExists("oauth2_signing_jwk")).isTrue();
        assertThat(tableExists("oauth2_signing_jwk_audit")).isTrue();
    }

    @Test
    void oidcAuthorizationSchemaShouldSupportRoundTrip() {
        jdbcTemplate.update("""
                INSERT INTO oauth2_registered_client (
                    id, client_id, client_name, client_authentication_methods,
                    authorization_grant_types, scopes, client_settings, token_settings
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "client-1",
                "resume-auth-integration",
                "Integration client",
                "client_secret_basic",
                "authorization_code,refresh_token",
                "openid,profile",
                "{\"settings\":\"client\"}",
                "{\"settings\":\"token\"}"
        );

        jdbcTemplate.update("""
                INSERT INTO oauth2_authorization (
                    id, registered_client_id, principal_name, authorization_grant_type,
                    authorized_scopes, attributes, access_token_value, access_token_metadata
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "authz-1",
                "client-1",
                "integration-user",
                "authorization_code",
                "openid profile",
                "{\"k\":\"v\"}",
                "{\"token\":\"value\"}",
                "{\"meta\":\"value\"}"
        );

        jdbcTemplate.update("""
                INSERT INTO oauth2_authorization_consent (
                    registered_client_id, principal_name, authorities
                ) VALUES (?, ?, ?)
                """,
                "client-1",
                "integration-user",
                "SCOPE_openid,SCOPE_profile"
        );

        Integer clients = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_registered_client WHERE id = ?",
                Integer.class,
                "client-1"
        );
        Integer authorizations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_authorization WHERE id = ?",
                Integer.class,
                "authz-1"
        );
        Integer consents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_authorization_consent WHERE registered_client_id = ?",
                Integer.class,
                "client-1"
        );
        assertThat(clients).isEqualTo(1);
        assertThat(authorizations).isEqualTo(1);
        assertThat(consents).isEqualTo(1);

        assertThat(columnDataType("oauth2_authorization", "attributes")).isEqualTo("text");
        assertThat(columnDataType("oauth2_authorization", "access_token_value")).isEqualTo("text");
        assertThat(columnDataType("oauth2_authorization", "access_token_metadata")).isEqualTo("text");
    }

    @Test
    void oidcSigningJwkAuditTriggerShouldCaptureInsertUpdateDelete() {
        jdbcTemplate.update("""
                INSERT INTO oauth2_signing_jwk (key_id, jwk_json, is_current)
                VALUES (?, ?, ?)
                """,
                "kid-integration-1",
                "ENC:v1:{\"kid\":\"kid-integration-1\"}",
                true
        );

        jdbcTemplate.update("""
                UPDATE oauth2_signing_jwk
                SET is_current = false, publish_until = now() + interval '1 day'
                WHERE key_id = ?
                """,
                "kid-integration-1"
        );

        jdbcTemplate.update("DELETE FROM oauth2_signing_jwk WHERE key_id = ?", "kid-integration-1");

        List<String> events = jdbcTemplate.queryForList(
                "SELECT event_type FROM oauth2_signing_jwk_audit ORDER BY id",
                String.class
        );
        assertThat(events).containsExactly("INSERT", "UPDATE", "DELETE");
    }

    @Test
    @Transactional
    void authUserRepositoryCrudAndQueriesShouldWork() {
        AuthUser authUser = new AuthUser();
        authUser.setId(1001L);
        authUser.setUid("owner-user");
        authUser.setPasswordHash("hash");
        authUser.setFirstName("Owner");
        authUser.setLastName("User");
        authUser.setCreated(Instant.now());
        authUser.setEnabled(true);

        authUserRepository.save(authUser);

        assertThat(authUserRepository.findByUid("owner-user")).isPresent();
        assertThat(authUserRepository.findByUidForUpdate("owner-user")).isPresent();
        assertThat(authUserRepository.existsByUid("owner-user")).isTrue();
    }

    @Test
    @Transactional
    void profileRestoreRepositoryCrudAndQueriesShouldWork() {
        ProfileRestore restore = new ProfileRestore();
        restore.setProfileId(2001L);
        restore.setToken("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        restore.setCreated(Instant.now());

        ProfileRestore saved = profileRestoreRepository.save(restore);

        assertThat(profileRestoreRepository.findByToken(saved.getToken())).isPresent();
        assertThat(profileRestoreRepository.findByProfileId(2001L)).isPresent();

        profileRestoreRepository.deleteByProfileId(2001L);
        assertThat(profileRestoreRepository.findByProfileId(2001L)).isEmpty();
    }

    @Test
    @Transactional
    void rememberMeTokenRepositoryCrudAndQueriesShouldWork() {
        RememberMeToken token = new RememberMeToken();
        token.setSeries("series-1");
        token.setToken("token-1");
        token.setLastUsed(Instant.now());
        token.setProfileId(3001L);
        token.setUsername("remember-user");

        rememberMeTokenRepository.save(token);

        assertThat(rememberMeTokenRepository.findBySeries("series-1")).isPresent();
        assertThat(rememberMeTokenRepository.deleteByUsername("remember-user")).isEqualTo(1);
        assertThat(rememberMeTokenRepository.findBySeries("series-1")).isEmpty();
    }

    @Test
    @Transactional
    void authOutboxRepositoryLockNextBatchShouldReturnEligibleEventsOnly() {
        Instant now = Instant.now();

        AuthOutboxEvent dueNew = new AuthOutboxEvent();
        dueNew.setEventType(AuthOutboxEventType.RESTORE_ACCESS_MAIL);
        dueNew.setPayload("{\"email\":\"one@example.com\"}");
        dueNew.setStatus(AuthOutboxStatus.NEW);
        dueNew.setAttempts(0);
        dueNew.setCreatedAt(now.minusSeconds(60));
        dueNew.setAvailableAt(now.minusSeconds(60));
        authOutboxRepository.save(dueNew);

        AuthOutboxEvent dueError = new AuthOutboxEvent();
        dueError.setEventType(AuthOutboxEventType.RESTORE_ACCESS_MAIL);
        dueError.setPayload("{\"email\":\"two@example.com\"}");
        dueError.setStatus(AuthOutboxStatus.ERROR);
        dueError.setAttempts(1);
        dueError.setCreatedAt(now.minusSeconds(60));
        dueError.setAvailableAt(now.minusSeconds(60));
        authOutboxRepository.save(dueError);

        AuthOutboxEvent notDue = new AuthOutboxEvent();
        notDue.setEventType(AuthOutboxEventType.RESTORE_ACCESS_MAIL);
        notDue.setPayload("{\"email\":\"future@example.com\"}");
        notDue.setStatus(AuthOutboxStatus.NEW);
        notDue.setAttempts(0);
        notDue.setCreatedAt(now.minusSeconds(60));
        notDue.setAvailableAt(now.plusSeconds(600));
        authOutboxRepository.save(notDue);

        AuthOutboxEvent exceededAttempts = new AuthOutboxEvent();
        exceededAttempts.setEventType(AuthOutboxEventType.RESTORE_ACCESS_MAIL);
        exceededAttempts.setPayload("{\"email\":\"max@example.com\"}");
        exceededAttempts.setStatus(AuthOutboxStatus.ERROR);
        exceededAttempts.setAttempts(3);
        exceededAttempts.setCreatedAt(now.minusSeconds(60));
        exceededAttempts.setAvailableAt(now.minusSeconds(60));
        authOutboxRepository.save(exceededAttempts);

        assertThat(authOutboxRepository.lockNextBatch(now, 10, 3))
                .extracting(AuthOutboxEvent::getPayload)
                .containsExactly(
                        "{\"email\":\"one@example.com\"}",
                        "{\"email\":\"two@example.com\"}"
                );
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private String columnDataType(String tableName, String columnName) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT data_type
                        FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                        """,
                String.class,
                tableName,
                columnName
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            AuthUser.class,
            ProfileRestore.class,
            RememberMeToken.class,
            AuthOutboxEvent.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            AuthUserRepository.class,
            ProfileRestoreRepository.class,
            RememberMeTokenRepository.class,
            AuthOutboxRepository.class
    })
    static class TestApp {
    }
}
