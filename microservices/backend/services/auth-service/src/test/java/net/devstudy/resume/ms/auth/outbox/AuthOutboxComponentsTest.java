package net.devstudy.resume.ms.auth.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.devstudy.resume.ms.auth.domain.entity.AuthOutboxEvent;
import net.devstudy.resume.ms.auth.domain.entity.AuthOutboxEventType;
import net.devstudy.resume.ms.auth.domain.entity.AuthOutboxStatus;
import net.devstudy.resume.ms.auth.adapters.outbox.AuthOutboxListener;
import net.devstudy.resume.ms.auth.adapters.outbox.AuthOutboxWriter;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.AuthOutboxRepository;
import net.devstudy.resume.ms.auth.ports.notification.event.RestoreAccessMailRequestedEvent;

class AuthOutboxComponentsTest {

    @Test
    void authOutboxWriterShouldPersistRestoreAccessMailEvent() {
        AuthOutboxRepository outboxRepository = mock(AuthOutboxRepository.class);
        AuthOutboxWriter authOutboxWriter = new AuthOutboxWriter(outboxRepository, new ObjectMapper());

        RestoreAccessMailRequestedEvent event = new RestoreAccessMailRequestedEvent(
                "user@example.com",
                "User",
                "https://app.local/restore/token"
        );

        authOutboxWriter.enqueueRestoreAccessMail(event);

        ArgumentCaptor<AuthOutboxEvent> savedEventCaptor = ArgumentCaptor.forClass(AuthOutboxEvent.class);
        verify(outboxRepository).save(savedEventCaptor.capture());
        AuthOutboxEvent saved = savedEventCaptor.getValue();
        assertThat(saved.getEventType()).isEqualTo(AuthOutboxEventType.RESTORE_ACCESS_MAIL);
        assertThat(saved.getStatus()).isEqualTo(AuthOutboxStatus.NEW);
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getPayload()).contains("user@example.com");
        assertThat(saved.getPayload()).contains("restore/token");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getAvailableAt()).isNotNull();
    }

    @Test
    void authOutboxWriterShouldIgnoreInvalidRestoreAccessMailEvent() {
        AuthOutboxRepository outboxRepository = mock(AuthOutboxRepository.class);
        AuthOutboxWriter authOutboxWriter = new AuthOutboxWriter(outboxRepository, new ObjectMapper());

        authOutboxWriter.enqueueRestoreAccessMail(new RestoreAccessMailRequestedEvent("", "User", "link"));
        authOutboxWriter.enqueueRestoreAccessMail(new RestoreAccessMailRequestedEvent("user@example.com", "User", ""));

        verify(outboxRepository, never()).save(org.mockito.ArgumentMatchers.any(AuthOutboxEvent.class));
    }

    @Test
    void authOutboxListenerShouldDelegateToWriter() {
        AuthOutboxWriter authOutboxWriter = mock(AuthOutboxWriter.class);
        AuthOutboxListener authOutboxListener = new AuthOutboxListener(authOutboxWriter);
        RestoreAccessMailRequestedEvent event = new RestoreAccessMailRequestedEvent(
                "user@example.com",
                "User",
                "https://app.local/restore/token"
        );

        authOutboxListener.onRestoreAccessMailRequested(event);

        verify(authOutboxWriter).enqueueRestoreAccessMail(event);
    }
}
