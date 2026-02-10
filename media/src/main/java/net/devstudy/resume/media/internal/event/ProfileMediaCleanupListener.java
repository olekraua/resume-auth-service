package net.devstudy.resume.media.internal.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.media.api.service.MediaCleanupService;
import net.devstudy.resume.shared.event.ProfileMediaCleanupRequestedEvent;

@Component
@RequiredArgsConstructor
public class ProfileMediaCleanupListener {

    private final MediaCleanupService mediaCleanupService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProfileMediaCleanupRequested(ProfileMediaCleanupRequestedEvent event) {
        if (event == null) {
            return;
        }
        if (event.clearCertificateTempLinks()) {
            mediaCleanupService.clearCertificateTempLinks();
        }
        if (event.photoUrls() != null && !event.photoUrls().isEmpty()) {
            mediaCleanupService.removePhotos(event.photoUrls());
        }
        if (event.certificateUrls() != null && !event.certificateUrls().isEmpty()) {
            mediaCleanupService.removeCertificates(event.certificateUrls());
        }
    }
}
