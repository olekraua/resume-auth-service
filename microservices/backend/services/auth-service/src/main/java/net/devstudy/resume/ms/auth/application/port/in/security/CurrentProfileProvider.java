package net.devstudy.resume.ms.auth.application.port.in.security;

import net.devstudy.resume.ms.auth.api.model.CurrentProfile;

/**
 * Provides access to the current authenticated profile from the security context.
 */
public interface CurrentProfileProvider {
    CurrentProfile getCurrentProfile();

    Long getCurrentId();
}
