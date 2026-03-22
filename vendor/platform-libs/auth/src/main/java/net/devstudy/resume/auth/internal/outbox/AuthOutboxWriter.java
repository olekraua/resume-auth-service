package net.devstudy.resume.auth.internal.outbox;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.internal.entity.AuthOutboxEvent;
import net.devstudy.resume.auth.internal.entity.AuthOutboxEventType;
import net.devstudy.resume.auth.internal.entity.AuthOutboxStatus;
import net.devstudy.resume.auth.internal.repository.storage.AuthOutboxRepository;
import net.devstudy.resume.notification.api.event.RestoreAccessMailRequestedEvent;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true")
public class AuthOutboxWriter {

    private final AuthOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void enqueueRestoreAccessMail(RestoreAccessMailRequestedEvent event) {
        if (event == null || event.email() == null || event.email().isBlank()) {
            return;
        }
        if (event.link() == null || event.link().isBlank()) {
            return;
        }
        String payload = writeJson(event);
        saveEvent(AuthOutboxEventType.RESTORE_ACCESS_MAIL, payload);
    }

    private void saveEvent(AuthOutboxEventType type, String payload) {
        Instant now = Instant.now();
        AuthOutboxEvent event = new AuthOutboxEvent();
        event.setEventType(type);
        event.setPayload(payload);
        event.setStatus(AuthOutboxStatus.NEW);
        event.setAttempts(0);
        event.setCreatedAt(now);
        event.setAvailableAt(now);
        outboxRepository.save(event);
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }
}
