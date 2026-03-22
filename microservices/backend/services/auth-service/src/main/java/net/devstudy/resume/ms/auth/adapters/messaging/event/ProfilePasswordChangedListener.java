package net.devstudy.resume.ms.auth.adapters.messaging.event;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.ms.auth.application.port.in.service.OidcAuthorizationRevocationService;
import net.devstudy.resume.ms.auth.domain.entity.AuthUser;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.AuthUserRepository;
import net.devstudy.resume.ms.auth.adapters.persistence.repository.storage.RememberMeTokenRepository;
import net.devstudy.resume.ms.auth.ports.profile.event.ProfilePasswordChangedEvent;

@Component
@RequiredArgsConstructor
public class ProfilePasswordChangedListener {

    private final RememberMeTokenRepository rememberMeTokenRepository;
    private final AuthUserRepository authUserRepository;
    private final ObjectProvider<OidcAuthorizationRevocationService> oidcAuthorizationRevocationServiceProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfilePasswordChanged(ProfilePasswordChangedEvent event) {
        if (event == null || event.profileId() == null) {
            return;
        }
        rememberMeTokenRepository.deleteByProfileId(event.profileId());
        OidcAuthorizationRevocationService oidcAuthorizationRevocationService =
                oidcAuthorizationRevocationServiceProvider.getIfAvailable();
        if (oidcAuthorizationRevocationService == null) {
            return;
        }
        authUserRepository.findById(event.profileId())
                .map(AuthUser::getUid)
                .ifPresent(oidcAuthorizationRevocationService::revokeAllByPrincipal);
    }
}
