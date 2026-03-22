package net.devstudy.resume.ms.auth.security;

import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.ms.auth.application.port.in.service.OidcAuthorizationRevocationService;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.oidc.enabled", havingValue = "true")
public class JdbcOidcAuthorizationRevocationService implements OidcAuthorizationRevocationService {

    private static final String DELETE_AUTHORIZATIONS_SQL =
            "DELETE FROM oauth2_authorization WHERE principal_name = ?";
    private static final String DELETE_CONSENTS_SQL =
            "DELETE FROM oauth2_authorization_consent WHERE principal_name = ?";

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void revokeAllByPrincipal(String principalName) {
        String normalizedPrincipal = normalize(principalName);
        if (!StringUtils.hasText(normalizedPrincipal)) {
            return;
        }
        jdbcTemplate.update(DELETE_AUTHORIZATIONS_SQL, normalizedPrincipal);
        jdbcTemplate.update(DELETE_CONSENTS_SQL, normalizedPrincipal);
    }

    private String normalize(String principalName) {
        if (!StringUtils.hasText(principalName)) {
            return null;
        }
        return principalName.trim().toLowerCase(Locale.ENGLISH);
    }
}
