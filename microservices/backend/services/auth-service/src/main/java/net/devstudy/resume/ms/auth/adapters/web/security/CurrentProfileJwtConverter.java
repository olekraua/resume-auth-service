package net.devstudy.resume.ms.auth.adapters.web.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import net.devstudy.resume.ms.auth.api.model.CurrentProfile;
import net.devstudy.resume.shared.constants.Constants;

@Component
public class CurrentProfileJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Long profileId = resolveProfileId(jwt.getClaim("profile_id"));
        String uid = jwt.getClaimAsString("uid");
        if (uid == null || uid.isBlank()) {
            uid = jwt.getSubject();
        }
        String fullName = jwt.getClaimAsString("name");
        if (fullName == null || fullName.isBlank()) {
            String first = jwt.getClaimAsString("first_name");
            String last = jwt.getClaimAsString("last_name");
            fullName = buildFullName(first, last);
        }
        CurrentProfile principal = new CurrentProfile(profileId, uid, fullName);
        return new UsernamePasswordAuthenticationToken(
                principal,
                jwt,
                resolveAuthorities(jwt)
        );
    }

    private List<GrantedAuthority> resolveAuthorities(Jwt jwt) {
        Set<String> authorityValues = new LinkedHashSet<>();
        authorityValues.add(Constants.UI.USER);
        addScopeAuthorities(jwt.getClaimAsString("scope"), authorityValues);
        Object scp = jwt.getClaims().get("scp");
        if (scp instanceof String scpValue) {
            addScopeAuthorities(scpValue, authorityValues);
        }
        return authorityValues.stream()
                .map(SimpleGrantedAuthority::new)
                .map(authority -> (GrantedAuthority) authority)
                .toList();
    }

    private void addScopeAuthorities(String rawScopes, Set<String> authorityValues) {
        if (rawScopes == null || rawScopes.isBlank()) {
            return;
        }
        for (String scope : rawScopes.trim().split("\\s+")) {
            if (scope.isBlank()) {
                continue;
            }
            authorityValues.add("SCOPE_" + scope.toLowerCase(Locale.ENGLISH));
        }
    }

    private Long resolveProfileId(Object claimValue) {
        if (claimValue instanceof Number number) {
            return number.longValue();
        }
        if (claimValue instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String buildFullName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last == null ? "" : last.trim();
        if (f.isEmpty()) {
            return l;
        }
        if (l.isEmpty()) {
            return f;
        }
        return f + " " + l;
    }
}
