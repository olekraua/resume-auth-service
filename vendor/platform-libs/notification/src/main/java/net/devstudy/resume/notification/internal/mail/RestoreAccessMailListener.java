package net.devstudy.resume.notification.internal.mail;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import net.devstudy.resume.notification.api.event.RestoreAccessMailRequestedEvent;
import net.devstudy.resume.notification.internal.service.RestoreAccessMailService;

@Component
public class RestoreAccessMailListener {

    private final RestoreAccessMailService mailService;

    public RestoreAccessMailListener(RestoreAccessMailService mailService) {
        this.mailService = mailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRestoreAccessMailRequested(RestoreAccessMailRequestedEvent event) {
        if (event == null) {
            return;
        }
        mailService.sendRestoreLink(event.email(), event.firstName(), event.link());
    }
}
