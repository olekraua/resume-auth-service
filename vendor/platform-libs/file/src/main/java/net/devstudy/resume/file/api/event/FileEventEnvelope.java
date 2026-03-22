package net.devstudy.resume.file.api.event;

import java.time.Instant;
import java.util.Map;

public record FileEventEnvelope(
        String eventId,
        String eventType,
        Instant occurredAt,
        String source,
        Map<String, Object> data) {
}
