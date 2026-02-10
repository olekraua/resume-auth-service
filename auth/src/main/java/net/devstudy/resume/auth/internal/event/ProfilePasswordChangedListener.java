package net.devstudy.resume.auth.internal.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.internal.repository.storage.RememberMeTokenRepository;
import net.devstudy.resume.profile.api.event.ProfilePasswordChangedEvent;

@Component
@RequiredArgsConstructor
public class ProfilePasswordChangedListener {

    private final RememberMeTokenRepository rememberMeTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfilePasswordChanged(ProfilePasswordChangedEvent event) {
        if (event == null || event.profileId() == null) {
            return;
        }
        rememberMeTokenRepository.deleteByProfileId(event.profileId());
    }
}
