package net.devstudy.resume.shared.middleware.web;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;

public class InMemoryIdempotencyStore {

    private final PlatformMiddlewareProperties middlewareProperties;
    private final LinkedHashMap<String, StoreEntry> entries = new LinkedHashMap<>(16, 0.75F, true);

    public InMemoryIdempotencyStore(PlatformMiddlewareProperties middlewareProperties) {
        this.middlewareProperties = middlewareProperties;
    }

    public synchronized AcquireResult tryAcquire(String cacheKey) {
        Instant now = Instant.now();
        evictExpired(now);
        StoreEntry existing = entries.get(cacheKey);
        if (existing == null) {
            entries.put(cacheKey, StoreEntry.inProgress(expiresAt(now)));
            trimToMaxSize();
            return AcquireResult.acquired();
        }
        if (existing.state == EntryState.IN_PROGRESS) {
            return AcquireResult.inProgress();
        }
        return AcquireResult.replay(existing.cachedResponse);
    }

    public synchronized void complete(String cacheKey, IdempotencyCachedResponse cachedResponse) {
        Instant now = Instant.now();
        evictExpired(now);
        StoreEntry existing = entries.get(cacheKey);
        if (existing == null || existing.state != EntryState.IN_PROGRESS) {
            return;
        }
        entries.put(cacheKey, StoreEntry.completed(expiresAt(now), cachedResponse));
        trimToMaxSize();
    }

    public synchronized void release(String cacheKey) {
        entries.remove(cacheKey);
    }

    private Instant expiresAt(Instant now) {
        return now.plus(middlewareProperties.getIdempotency().getTtl());
    }

    private void evictExpired(Instant now) {
        Iterator<Map.Entry<String, StoreEntry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, StoreEntry> entry = iterator.next();
            if (entry.getValue().expiresAt.isAfter(now)) {
                continue;
            }
            iterator.remove();
        }
    }

    private void trimToMaxSize() {
        int maxEntries = middlewareProperties.getIdempotency().getMaxEntries();
        while (entries.size() > maxEntries) {
            Iterator<Map.Entry<String, StoreEntry>> iterator = entries.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private enum EntryState {
        IN_PROGRESS,
        COMPLETED
    }

    private static final class StoreEntry {

        private final EntryState state;
        private final Instant expiresAt;
        private final IdempotencyCachedResponse cachedResponse;

        private StoreEntry(EntryState state, Instant expiresAt, IdempotencyCachedResponse cachedResponse) {
            this.state = state;
            this.expiresAt = expiresAt;
            this.cachedResponse = cachedResponse;
        }

        private static StoreEntry inProgress(Instant expiresAt) {
            return new StoreEntry(EntryState.IN_PROGRESS, expiresAt, null);
        }

        private static StoreEntry completed(Instant expiresAt, IdempotencyCachedResponse response) {
            return new StoreEntry(EntryState.COMPLETED, expiresAt, response);
        }
    }

    public static final class AcquireResult {

        private final Decision decision;
        private final IdempotencyCachedResponse cachedResponse;

        private AcquireResult(Decision decision, IdempotencyCachedResponse cachedResponse) {
            this.decision = decision;
            this.cachedResponse = cachedResponse;
        }

        public static AcquireResult acquired() {
            return new AcquireResult(Decision.ACQUIRED, null);
        }

        public static AcquireResult inProgress() {
            return new AcquireResult(Decision.IN_PROGRESS, null);
        }

        public static AcquireResult replay(IdempotencyCachedResponse cachedResponse) {
            return new AcquireResult(Decision.REPLAY, cachedResponse);
        }

        public Decision getDecision() {
            return decision;
        }

        public IdempotencyCachedResponse getCachedResponse() {
            return cachedResponse;
        }
    }

    public enum Decision {
        ACQUIRED,
        IN_PROGRESS,
        REPLAY
    }
}
