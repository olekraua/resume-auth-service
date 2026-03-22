package net.devstudy.resume.ms.auth.application.service.impl;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.ms.auth.api.model.CurrentProfile;
import net.devstudy.resume.ms.auth.application.port.in.service.ProfileAccountService;
import net.devstudy.resume.ms.auth.application.security.LoginProtectionService;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileAuthResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentProfileDetailsService implements UserDetailsService {

    private final ProfileAccountService profileAccountService;
    private final LoginProtectionService loginProtectionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        loginProtectionService.assertLoginAllowed(username);
        ProfileAuthResponse auth = profileAccountService.loadForAuth(username);
        if (auth == null || auth.uid() == null) {
            throw new UsernameNotFoundException("Profile not found: " + username);
        }
        return new CurrentProfile(auth);
    }
}
