package net.devstudy.resume.web.security;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import net.devstudy.resume.auth.api.model.CurrentProfile;
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
                List.of(new SimpleGrantedAuthority(Constants.UI.USER))
        );
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
