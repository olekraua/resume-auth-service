package net.devstudy.resume.auth.internal.security;

import net.devstudy.resume.auth.api.model.CurrentProfile;
import net.devstudy.resume.auth.api.security.CurrentProfileProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.security.session.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityContextCurrentProfileProvider implements CurrentProfileProvider {

    @Override
    public CurrentProfile getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentProfile currentProfile)) {
            return null;
        }
        return currentProfile;
    }

    @Override
    public Long getCurrentId() {
        CurrentProfile currentProfile = getCurrentProfile();
        return currentProfile != null ? currentProfile.getId() : null;
    }
}
