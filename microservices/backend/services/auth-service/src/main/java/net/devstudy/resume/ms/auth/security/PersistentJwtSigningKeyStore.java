package net.devstudy.resume.ms.auth.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

@Component
@ConditionalOnProperty(name = "app.security.oidc.enabled", havingValue = "true")
public class PersistentJwtSigningKeyStore {

    private static final long KEY_LIFECYCLE_LOCK_ID = 908144260459L;
    private static final int MAX_KEYSET_LOAD_ATTEMPTS = 20;
    private static final long RETRY_DELAY_MILLIS = 100L;
    private static final String ENCRYPTED_VALUE_PREFIX = "ENC:v1:";
    private static final int GCM_IV_SIZE_BYTES = 12;
    private static final int GCM_TAG_SIZE_BITS = 128;
    private static final int EXPECTED_AES256_KEY_SIZE_BYTES = 32;

    private static final String READ_CURRENT_KEY_SQL = """
            SELECT key_id, jwk_json, created_at
            FROM public.oauth2_signing_jwk
            WHERE is_current = true
            ORDER BY created_at DESC
            LIMIT 1
            """;

    private static final String READ_ACTIVE_KEYS_SQL = """
            SELECT jwk_json
            FROM public.oauth2_signing_jwk
            WHERE is_current = true
               OR (publish_until IS NOT NULL AND publish_until > CURRENT_TIMESTAMP)
            ORDER BY is_current DESC, created_at DESC
            """;

    private static final String INSERT_CURRENT_KEY_SQL = """
            INSERT INTO public.oauth2_signing_jwk (key_id, jwk_json, is_current, publish_until)
            VALUES (?, ?, true, NULL)
            ON CONFLICT (key_id) DO NOTHING
            """;

    private static final String ENCRYPT_CURRENT_KEY_SQL = """
            UPDATE public.oauth2_signing_jwk
            SET jwk_json = ?
            WHERE key_id = ?
              AND is_current = true
            """;

    private static final String DEMOTE_CURRENT_KEY_SQL = """
            UPDATE public.oauth2_signing_jwk
            SET is_current = false,
                publish_until = ?,
                jwk_json = ?
            WHERE key_id = ?
              AND is_current = true
            """;

    private static final String DELETE_EXPIRED_RETIRED_KEYS_SQL = """
            DELETE FROM public.oauth2_signing_jwk
            WHERE is_current = false
              AND publish_until IS NOT NULL
              AND publish_until <= ?
            """;

    private static final String TRY_ADVISORY_XACT_LOCK_SQL = "SELECT pg_try_advisory_xact_lock(?)";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Duration rotationInterval;
    private final Duration signedTokenTtl;
    private final Duration clockSkew;
    private final Clock clock;
    private final SecretKeySpec encryptionKey;
    private final SecureRandom secureRandom;

    public PersistentJwtSigningKeyStore(JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            @Value("${app.security.oidc.signing-key.rotation-interval:PT24H}") Duration rotationInterval,
            @Value("${app.security.oidc.signing-key.token-ttl:${app.security.oidc.access-token-ttl:PT10M}}") Duration signedTokenTtl,
            @Value("${app.security.oidc.signing-key.clock-skew:PT1M}") Duration clockSkew,
            @Value("${app.security.oidc.signing-key.encryption-key:}") String encryptionKeyBase64) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.rotationInterval = rotationInterval;
        this.signedTokenTtl = signedTokenTtl;
        this.clockSkew = clockSkew;
        this.clock = Clock.systemUTC();
        this.encryptionKey = createEncryptionKey(encryptionKeyBase64);
        this.secureRandom = new SecureRandom();
    }

    public JWKSet loadOrCreateSigningJwkSet() {
        for (int attempt = 0; attempt < MAX_KEYSET_LOAD_ATTEMPTS; attempt++) {
            maintainKeyLifecycleIfLockAcquired();
            List<RSAKey> activeKeys = readActiveSigningKeys();
            if (!activeKeys.isEmpty()) {
                return new JWKSet(new ArrayList<>(activeKeys));
            }
            pauseBeforeRetry();
        }
        throw new IllegalStateException("Failed to load persistent JWT signing key set");
    }

    public void verifyStoreAvailabilityOrFailFast() {
        try {
            JWKSet keySet = loadOrCreateSigningJwkSet();
            if (keySet.getKeys().isEmpty()) {
                throw new IllegalStateException("Signing key set is empty");
            }
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "OIDC signing key store is unavailable. Refusing to start with ephemeral signing keys.",
                    ex
            );
        }
    }

    private void maintainKeyLifecycleIfLockAcquired() {
        transactionTemplate.executeWithoutResult(status -> {
            if (!tryAcquireLifecycleLock()) {
                return;
            }
            CurrentSigningKey current = readCurrentSigningKey();
            if (current == null) {
                createInitialCurrentSigningKey();
                return;
            }
            if (shouldRotate(current.createdAt())) {
                rotateCurrentSigningKey(current);
            }
            cleanupExpiredRetiredKeys();
        });
    }

    private boolean tryAcquireLifecycleLock() {
        Boolean acquired = jdbcTemplate.queryForObject(
                TRY_ADVISORY_XACT_LOCK_SQL,
                Boolean.class,
                KEY_LIFECYCLE_LOCK_ID
        );
        return Boolean.TRUE.equals(acquired);
    }

    private CurrentSigningKey readCurrentSigningKey() {
        List<CurrentSigningKey> rows = jdbcTemplate.query(
                READ_CURRENT_KEY_SQL,
                (rs, rowNum) -> {
                    String keyId = rs.getString("key_id");
                    ParsedRsaKey parsed = parseStoredRsaKey(rs.getString("jwk_json"), true, true);
                    return new CurrentSigningKey(
                            keyId,
                            parsed.key(),
                            parsed.encryptedAtRest(),
                            rs.getTimestamp("created_at").toInstant()
                    );
                }
        );
        if (rows.isEmpty()) {
            return null;
        }
        CurrentSigningKey current = rows.get(0);
        if (current.encryptedAtRest()) {
            return current;
        }
        String encryptedPayload = encryptJwkJson(current.key().toJSONString());
        int updated = jdbcTemplate.update(
                ENCRYPT_CURRENT_KEY_SQL,
                encryptedPayload,
                current.keyId()
        );
        if (updated != 1) {
            throw new IllegalStateException("Failed to enforce encryption-at-rest for current signing key");
        }
        return new CurrentSigningKey(
                current.keyId(),
                current.key(),
                true,
                current.createdAt()
        );
    }

    private List<RSAKey> readActiveSigningKeys() {
        List<String> jsonKeys = jdbcTemplate.query(
                READ_ACTIVE_KEYS_SQL,
                (rs, rowNum) -> rs.getString(1)
        );
        List<RSAKey> parsedKeys = new ArrayList<>(jsonKeys.size());
        for (String json : jsonKeys) {
            parsedKeys.add(parseStoredRsaKey(json, false, false).key());
        }
        return parsedKeys;
    }

    private void createInitialCurrentSigningKey() {
        RSAKey generated = generateRsa();
        jdbcTemplate.update(
                INSERT_CURRENT_KEY_SQL,
                generated.getKeyID(),
                encryptJwkJson(generated.toJSONString())
        );
    }

    private boolean shouldRotate(Instant createdAt) {
        return !createdAt.plus(rotationInterval).isAfter(clock.instant());
    }

    private void rotateCurrentSigningKey(CurrentSigningKey current) {
        RSAKey nextKey = generateRsa();
        Instant now = clock.instant();
        Timestamp publishUntil = Timestamp.from(now.plus(signedTokenTtl).plus(clockSkew));
        int demoted = jdbcTemplate.update(
                DEMOTE_CURRENT_KEY_SQL,
                publishUntil,
                current.key().toPublicJWK().toJSONString(),
                current.key().getKeyID()
        );
        if (demoted == 0) {
            return;
        }
        jdbcTemplate.update(
                INSERT_CURRENT_KEY_SQL,
                nextKey.getKeyID(),
                encryptJwkJson(nextKey.toJSONString())
        );
    }

    private void cleanupExpiredRetiredKeys() {
        jdbcTemplate.update(
                DELETE_EXPIRED_RETIRED_KEYS_SQL,
                Timestamp.from(clock.instant())
        );
    }

    private void pauseBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for signing keys", ex);
        }
    }

    private ParsedRsaKey parseStoredRsaKey(String payload, boolean requirePrivate, boolean allowUnencryptedPrivate) {
        String jwkJson = payload;
        boolean encrypted = isEncryptedPayload(payload);
        if (encrypted) {
            jwkJson = decryptJwkJson(payload);
        }
        try {
            JWK parsed = JWK.parse(jwkJson);
            if (!(parsed instanceof RSAKey rsaKey)) {
                throw new IllegalStateException("Stored JWT signing key is not RSA");
            }
            if (!allowUnencryptedPrivate && !encrypted && rsaKey.toPrivateKey() != null) {
                throw new IllegalStateException("Unencrypted private signing key is not allowed at rest");
            }
            if (requirePrivate && rsaKey.toPrivateKey() == null) {
                throw new IllegalStateException("Current JWT signing key has no private part");
            }
            return new ParsedRsaKey(rsaKey, encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse stored JWT signing key", ex);
        }
    }

    private boolean isEncryptedPayload(String payload) {
        return payload != null && payload.startsWith(ENCRYPTED_VALUE_PREFIX);
    }

    private String encryptJwkJson(String jwkJson) {
        try {
            byte[] iv = new byte[GCM_IV_SIZE_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_SIZE_BITS, iv));
            byte[] cipherText = cipher.doFinal(jwkJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return ENCRYPTED_VALUE_PREFIX
                    + Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt JWT signing key", ex);
        }
    }

    private String decryptJwkJson(String encryptedPayload) {
        String[] parts = encryptedPayload.split(":", 4);
        if (parts.length != 4 || !"ENC".equals(parts[0]) || !"v1".equals(parts[1])) {
            throw new IllegalStateException("Unsupported encrypted key payload format");
        }
        try {
            byte[] iv = Base64.getDecoder().decode(parts[2]);
            byte[] cipherText = Base64.getDecoder().decode(parts[3]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_SIZE_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt JWT signing key payload", ex);
        }
    }

    private SecretKeySpec createEncryptionKey(String encryptionKeyBase64) {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.isBlank()) {
            throw new IllegalStateException(
                    "Signing key encryption key is required: set app.security.oidc.signing-key.encryption-key"
            );
        }
        try {
            byte[] rawKey = Base64.getDecoder().decode(encryptionKeyBase64);
            if (rawKey.length != EXPECTED_AES256_KEY_SIZE_BYTES) {
                throw new IllegalStateException("Signing key encryption key must be 32 bytes (base64-encoded)");
            }
            return new SecretKeySpec(rawKey, "AES");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Signing key encryption key is not valid base64", ex);
        }
    }

    private RSAKey generateRsa() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key", ex);
        }
    }

    private record ParsedRsaKey(RSAKey key, boolean encryptedAtRest) {
    }

    private record CurrentSigningKey(String keyId,
            RSAKey key,
            boolean encryptedAtRest,
            Instant createdAt) {
    }
}
