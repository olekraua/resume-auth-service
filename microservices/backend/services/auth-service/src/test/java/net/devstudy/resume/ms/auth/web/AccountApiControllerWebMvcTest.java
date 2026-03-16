package net.devstudy.resume.ms.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import net.devstudy.resume.auth.api.model.CurrentProfile;
import net.devstudy.resume.auth.api.security.CurrentProfileProvider;
import net.devstudy.resume.auth.api.service.ProfileAccountService;
import net.devstudy.resume.auth.api.service.UidSuggestionService;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfilePasswordUpdateRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileUidUpdateRequest;
import net.devstudy.resume.profile.api.exception.UidAlreadyExistsException;
import net.devstudy.resume.web.controller.api.AccountApiController;
import net.devstudy.resume.web.security.RememberMeSupport;

@WebMvcTest(controllers = AccountApiController.class, properties = "app.security.session.enabled=true")
@AutoConfigureMockMvc(addFilters = false)
class AccountApiControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private CurrentProfileProvider currentProfileProvider;

    @MockBean
    private UidSuggestionService uidSuggestionService;

    @MockBean
    private RememberMeSupport rememberMeSupport;

    @MockBean
    private ProfileAccountService profileAccountService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changePasswordShouldReturnNoContentWhenRequestIsValid() throws Exception {
        CurrentProfile currentProfile = new CurrentProfile(1L, "john", "John Doe");
        when(currentProfileProvider.getCurrentId()).thenReturn(1L);
        when(currentProfileProvider.getCurrentProfile()).thenReturn(currentProfile);
        when(profileAccountService.loadForAuth("john"))
                .thenReturn(new ProfileAuthResponse(1L, "john", "encoded", "John", "Doe", null, null));
        when(passwordEncoder.matches("OldSecret123", "encoded")).thenReturn(true);

        mockMvc.perform(post("/api/account/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("OldSecret123", "NewSecret123")))
                .andExpect(status().isNoContent());

        verify(profileAccountService).updatePassword(1L, new ProfilePasswordUpdateRequest("NewSecret123"));
    }

    @Test
    void changePasswordShouldReturnUnauthorizedWhenCurrentUserMissing() throws Exception {
        when(currentProfileProvider.getCurrentId()).thenReturn(null);

        mockMvc.perform(post("/api/account/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("OldSecret123", "NewSecret123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void changePasswordShouldReturnBadRequestWhenCurrentPasswordInvalid() throws Exception {
        CurrentProfile currentProfile = new CurrentProfile(1L, "john", "John Doe");
        when(currentProfileProvider.getCurrentId()).thenReturn(1L);
        when(currentProfileProvider.getCurrentProfile()).thenReturn(currentProfile);
        when(profileAccountService.loadForAuth("john"))
                .thenReturn(new ProfileAuthResponse(1L, "john", "encoded", "John", "Doe", null, null));
        when(passwordEncoder.matches("OldSecret123", "encoded")).thenReturn(false);

        mockMvc.perform(post("/api/account/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("OldSecret123", "NewSecret123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is invalid"))
                .andExpect(jsonPath("$.errors[0].field").value("currentPassword"));
    }

    @Test
    void changeLoginShouldReturnOkWhenUidIsChanged() throws Exception {
        when(currentProfileProvider.getCurrentId()).thenReturn(1L);

        mockMvc.perform(post("/api/account/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeLoginJson("new_uid")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUid").value("new_uid"))
                .andExpect(jsonPath("$.reloginRequired").value(true));

        verify(profileAccountService).updateUid(eq(1L), eq(new ProfileUidUpdateRequest("new_uid")));
        verify(rememberMeSupport).logout(any(), any(), nullable(Authentication.class));
    }

    @Test
    void changeLoginShouldReturnConflictAndSuggestionsWhenUidExists() throws Exception {
        when(currentProfileProvider.getCurrentId()).thenReturn(1L);
        doThrow(new UidAlreadyExistsException("new_uid"))
                .when(profileAccountService)
                .updateUid(1L, new ProfileUidUpdateRequest("new_uid"));
        when(uidSuggestionService.suggest("new_uid")).thenReturn(List.of("new_uid1", "new_uid2"));

        mockMvc.perform(post("/api/account/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeLoginJson("new_uid")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("Uid already exists: new_uid"))
                .andExpect(jsonPath("$.uidSuggestions[0]").value("new_uid1"));
    }

    @Test
    void removeAccountShouldReturnUnauthorizedWhenCurrentUserMissing() throws Exception {
        when(currentProfileProvider.getCurrentId()).thenReturn(null);

        mockMvc.perform(delete("/api/account/remove"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void removeAccountShouldReturnNoContentWhenCurrentUserExists() throws Exception {
        when(currentProfileProvider.getCurrentId()).thenReturn(1L);

        mockMvc.perform(delete("/api/account/remove"))
                .andExpect(status().isNoContent());

        verify(profileAccountService).removeProfile(1L);
        verify(rememberMeSupport).logout(any(), any(), nullable(Authentication.class));
    }

    private static String changePasswordJson(String currentPassword, String newPassword) {
        return """
                {
                  "currentPassword": "%s",
                  "newPassword": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(currentPassword, newPassword, newPassword);
    }

    private static String changeLoginJson(String newUid) {
        return """
                {
                  "newUid": "%s"
                }
                """.formatted(newUid);
    }
}
