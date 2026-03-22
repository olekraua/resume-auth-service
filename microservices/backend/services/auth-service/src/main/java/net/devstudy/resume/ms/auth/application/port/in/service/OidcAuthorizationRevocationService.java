package net.devstudy.resume.ms.auth.application.port.in.service;

/**
 * Revokes persisted OIDC/OAuth2 authorizations for a principal.
 */
public interface OidcAuthorizationRevocationService {

    void revokeAllByPrincipal(String principalName);
}
