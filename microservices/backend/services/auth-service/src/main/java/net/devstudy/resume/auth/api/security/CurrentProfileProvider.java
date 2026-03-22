package net.devstudy.resume.auth.api.security;

import net.devstudy.resume.auth.api.model.CurrentProfile;

/**
 * Provides access to the current authenticated profile from the security context.
 */
public interface CurrentProfileProvider {
    CurrentProfile getCurrentProfile();

    Long getCurrentId();
}
