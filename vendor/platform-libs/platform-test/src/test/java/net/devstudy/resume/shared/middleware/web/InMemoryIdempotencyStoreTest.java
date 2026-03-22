package net.devstudy.resume.shared.middleware.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;
import net.devstudy.resume.shared.middleware.web.InMemoryIdempotencyStore.AcquireResult;
import net.devstudy.resume.shared.middleware.web.InMemoryIdempotencyStore.Decision;

class InMemoryIdempotencyStoreTest {

    private InMemoryIdempotencyStore idempotencyStore;

    @BeforeEach
    void setUp() {
        PlatformMiddlewareProperties middlewareProperties = new PlatformMiddlewareProperties();
        middlewareProperties.getIdempotency().setTtl(Duration.ofMinutes(5));
        middlewareProperties.getIdempotency().setMaxEntries(2);
        idempotencyStore = new InMemoryIdempotencyStore(middlewareProperties);
    }

    @Test
    void shouldReplayCompletedResponse() {
        String cacheKey = "POST|/api/test|demo|abc";

        AcquireResult firstAcquire = idempotencyStore.tryAcquire(cacheKey);
        idempotencyStore.complete(cacheKey, createResponse(201, "created"));
        AcquireResult secondAcquire = idempotencyStore.tryAcquire(cacheKey);

        assertThat(firstAcquire.getDecision()).isEqualTo(Decision.ACQUIRED);
        assertThat(secondAcquire.getDecision()).isEqualTo(Decision.REPLAY);
        assertThat(secondAcquire.getCachedResponse()).isNotNull();
        assertThat(secondAcquire.getCachedResponse().status()).isEqualTo(201);
    }

    @Test
    void shouldReturnInProgressForConcurrentRequest() {
        String cacheKey = "POST|/api/test|demo|same";

        AcquireResult firstAcquire = idempotencyStore.tryAcquire(cacheKey);
        AcquireResult secondAcquire = idempotencyStore.tryAcquire(cacheKey);

        assertThat(firstAcquire.getDecision()).isEqualTo(Decision.ACQUIRED);
        assertThat(secondAcquire.getDecision()).isEqualTo(Decision.IN_PROGRESS);
    }

    @Test
    void shouldEvictOldestEntriesWhenSizeLimitExceeded() {
        String key1 = "POST|/api/test|demo|k1";
        String key2 = "POST|/api/test|demo|k2";
        String key3 = "POST|/api/test|demo|k3";

        idempotencyStore.tryAcquire(key1);
        idempotencyStore.complete(key1, createResponse(200, "one"));
        idempotencyStore.tryAcquire(key2);
        idempotencyStore.complete(key2, createResponse(200, "two"));
        idempotencyStore.tryAcquire(key3);
        idempotencyStore.complete(key3, createResponse(200, "three"));

        AcquireResult evictedResult = idempotencyStore.tryAcquire(key1);
        AcquireResult preservedResult = idempotencyStore.tryAcquire(key3);

        assertThat(evictedResult.getDecision()).isEqualTo(Decision.ACQUIRED);
        assertThat(preservedResult.getDecision()).isEqualTo(Decision.REPLAY);
    }

    private IdempotencyCachedResponse createResponse(int status, String payload) {
        return new IdempotencyCachedResponse(
                status,
                Map.of("Content-Type", List.of("application/json")),
                payload.getBytes(StandardCharsets.UTF_8));
    }
}
