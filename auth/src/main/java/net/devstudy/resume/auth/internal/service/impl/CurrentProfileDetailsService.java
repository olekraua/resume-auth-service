package net.devstudy.resume.auth.internal.service.impl;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.api.model.CurrentProfile;
import net.devstudy.resume.auth.api.service.ProfileAccountService;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentProfileDetailsService implements UserDetailsService {

    private final ProfileAccountService profileAccountService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ProfileAuthResponse auth = profileAccountService.loadForAuth(username);
        if (auth == null || auth.uid() == null) {
            throw new UsernameNotFoundException("Profile not found: " + username);
        }
        return new CurrentProfile(auth);
    }
}
