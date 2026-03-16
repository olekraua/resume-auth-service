package net.devstudy.resume.auth.internal.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import net.devstudy.resume.auth.api.model.CurrentProfile;

public final class SecurityUtil {
    private SecurityUtil() {
    }

    public static CurrentProfile getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentProfile currentProfile)) {
            return null;
        }
        return currentProfile;
    }

    public static Long getCurrentId() {
        CurrentProfile currentProfile = getCurrentProfile();
        return currentProfile != null ? currentProfile.getId() : null;
    }
}
