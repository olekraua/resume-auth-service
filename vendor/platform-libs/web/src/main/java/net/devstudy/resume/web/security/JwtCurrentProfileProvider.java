package net.devstudy.resume.web.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import net.devstudy.resume.auth.api.model.CurrentProfile;
import net.devstudy.resume.auth.api.security.CurrentProfileProvider;

@Component
@ConditionalOnProperty(name = "app.security.jwt.enabled", havingValue = "true")
public class JwtCurrentProfileProvider implements CurrentProfileProvider {

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
