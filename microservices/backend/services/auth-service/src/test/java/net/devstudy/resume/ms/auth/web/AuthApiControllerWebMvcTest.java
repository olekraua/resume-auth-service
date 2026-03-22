package net.devstudy.resume.ms.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import net.devstudy.resume.ms.auth.api.model.CurrentProfile;
import net.devstudy.resume.ms.auth.application.port.in.security.CurrentProfileProvider;
import net.devstudy.resume.ms.auth.application.security.LoginLockedException;
import net.devstudy.resume.ms.auth.application.security.LoginProtectionService;
import net.devstudy.resume.ms.auth.adapters.web.controller.api.AuthApiController;
import net.devstudy.resume.ms.auth.adapters.web.security.RememberMeSupport;

@WebMvcTest(controllers = AuthApiController.class, properties = "app.security.session.enabled=true")
@AutoConfigureMockMvc(addFilters = false)
class AuthApiControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentProfileProvider currentProfileProvider;

    @MockitoBean
    private AuthenticationConfiguration authenticationConfiguration;

    @MockitoBean
    private RememberMeSupport rememberMeSupport;

    @MockitoBean
    private LoginProtectionService loginProtectionService;

    private final AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginShouldAuthenticateAndReturnSessionResponse() throws Exception {
        CurrentProfile currentProfile = new CurrentProfile(1L, "john", "John Doe");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                currentProfile,
                null,
                currentProfile.getAuthorities()
        );
        when(currentProfileProvider.getCurrentProfile()).thenReturn(null);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson("john", "Secret123", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.uid").value("john"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));

        verify(loginProtectionService).assertLoginAllowed("john");
        verify(loginProtectionService).onAuthenticationSuccess("john");
        verify(rememberMeSupport).loginSuccess(any(), any(), any(Authentication.class), eq(true));
    }

    @Test
    void loginShouldReturnBadRequestForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void loginShouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        when(currentProfileProvider.getCurrentProfile()).thenReturn(null);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson("john", "bad", false)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        verify(loginProtectionService).onAuthenticationFailure("john");
    }

    @Test
    void loginShouldReturnTooManyRequestsWhenLockoutIsActive() throws Exception {
        when(currentProfileProvider.getCurrentProfile()).thenReturn(null);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        Mockito.doThrow(new LoginLockedException(75L)).when(loginProtectionService).assertLoginAllowed("john");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson("john", "Secret123", false)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "75"))
                .andExpect(jsonPath("$.message").value("Too many failed login attempts. Try again later."));
    }

    @Test
    void loginShouldReturnInternalServerErrorWhenAuthenticationManagerUnavailable() throws Exception {
        when(currentProfileProvider.getCurrentProfile()).thenReturn(null);
        when(authenticationConfiguration.getAuthenticationManager()).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson("john", "Secret123", false)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Authentication unavailable"));
    }

    @Test
    void logoutShouldClearSecurityContextAndReturnNoContent() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("john", "n/a"));

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verify(rememberMeSupport).logout(any(), any(), any(Authentication.class));
    }

    private static String loginRequestJson(String username, String password, boolean rememberMe) {
        return """
                {
                  "username": "%s",
                  "password": "%s",
                  "rememberMe": %s
                }
                """.formatted(username, password, rememberMe);
    }
}
