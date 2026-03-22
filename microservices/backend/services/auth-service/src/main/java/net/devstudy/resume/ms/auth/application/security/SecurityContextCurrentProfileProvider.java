package net.devstudy.resume.ms.auth.application.security;

import net.devstudy.resume.ms.auth.api.model.CurrentProfile;
import net.devstudy.resume.ms.auth.application.port.in.security.CurrentProfileProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
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
