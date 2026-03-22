package net.devstudy.resume.ms.auth.adapters.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.ms.auth.ports.notification.event.RestoreAccessMailRequestedEvent;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true")
public class AuthOutboxListener {

    private final AuthOutboxWriter outboxWriter;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onRestoreAccessMailRequested(RestoreAccessMailRequestedEvent event) {
        outboxWriter.enqueueRestoreAccessMail(event);
    }
}
