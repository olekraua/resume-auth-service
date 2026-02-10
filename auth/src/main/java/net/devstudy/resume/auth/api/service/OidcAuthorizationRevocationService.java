package net.devstudy.resume.auth.api.service;

/**
 * Revokes persisted OIDC/OAuth2 authorizations for a principal.
 */
public interface OidcAuthorizationRevocationService {

    void revokeAllByPrincipal(String principalName);
}
