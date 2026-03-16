package net.devstudy.resume.auth.api.model;

import java.io.Serial;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import net.devstudy.resume.shared.constants.Constants;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.model.Profile;

public final class CurrentProfile extends User {
    @Serial
    private static final long serialVersionUID = 3850489832510630519L;

    private final Long id;
    private final String fullName;

    public CurrentProfile(ProfileAuthResponse auth) {
        super(
            auth.uid(),
            auth.passwordHash(),
            true,  // enabled
            true,  // accountNonExpired
            true,  // credentialsNonExpired
            true,  // accountNonLocked
            List.of(new SimpleGrantedAuthority(Constants.UI.USER))
        );
        this.id = auth.id();
        this.fullName = buildFullName(auth.firstName(), auth.lastName());
    }

    public CurrentProfile(Profile profile) {
        super(
            profile.getUid(),
            "",
            true,  // enabled
            true,  // accountNonExpired
            true,  // credentialsNonExpired
            true,  // accountNonLocked
            List.of(new SimpleGrantedAuthority(Constants.UI.USER))
        );
        this.id = profile.getId();
        this.fullName = profile.getFullName();
    }

    public CurrentProfile(Long id, String uid, String fullName) {
        super(
            uid == null ? "" : uid,
            "",
            true,
            true,
            true,
            true,
            List.of(new SimpleGrantedAuthority(Constants.UI.USER))
        );
        this.id = id;
        this.fullName = fullName == null ? "" : fullName.trim();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    private String buildFullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }

    @Override
    public String toString() {
        return String.format("CurrentProfile [id=%s, username=%s]", id, getUsername());
    }
}
