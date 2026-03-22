package net.devstudy.resume.profile.internal.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.event.ProfileIndexingRequestedEvent;
import net.devstudy.resume.profile.api.event.ProfileSearchRemovalRequestedEvent;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true")
public class ProfileOutboxListener {

    private final ProfileOutboxWriter outboxWriter;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onProfileIndexingRequested(ProfileIndexingRequestedEvent event) {
        if (event == null || event.snapshot() == null) {
            return;
        }
        outboxWriter.enqueueIndexing(event.snapshot());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onProfileRemoval(ProfileSearchRemovalRequestedEvent event) {
        if (event == null || event.profileId() == null) {
            return;
        }
        outboxWriter.enqueueRemoval(event.profileId());
    }
}
