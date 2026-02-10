package net.devstudy.resume.profile.internal.outbox;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.event.ProfileIndexingSnapshot;
import net.devstudy.resume.profile.api.model.ProfileOutboxEvent;
import net.devstudy.resume.profile.api.model.ProfileOutboxEventType;
import net.devstudy.resume.profile.api.model.ProfileOutboxStatus;
import net.devstudy.resume.profile.internal.repository.storage.ProfileOutboxRepository;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true")
public class ProfileOutboxWriter {

    private final ProfileOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void enqueueIndexing(ProfileIndexingSnapshot snapshot) {
        if (snapshot == null || snapshot.profileId() == null) {
            return;
        }
        String payload = writeJson(snapshot);
        saveEvent(snapshot.profileId(), ProfileOutboxEventType.PROFILE_INDEX, payload);
    }

    public void enqueueRemoval(Long profileId) {
        if (profileId == null) {
            return;
        }
        String payload = writeJson(new RemovalPayload(profileId));
        saveEvent(profileId, ProfileOutboxEventType.PROFILE_REMOVE, payload);
    }

    private void saveEvent(Long profileId, ProfileOutboxEventType type, String payload) {
        Instant now = Instant.now();
        ProfileOutboxEvent event = new ProfileOutboxEvent();
        event.setProfileId(profileId);
        event.setEventType(type);
        event.setPayload(payload);
        event.setStatus(ProfileOutboxStatus.NEW);
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

    public record RemovalPayload(Long profileId) {
    }
}
