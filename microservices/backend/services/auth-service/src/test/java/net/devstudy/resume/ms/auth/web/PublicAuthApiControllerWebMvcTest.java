package net.devstudy.resume.ms.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import net.devstudy.resume.ms.auth.api.model.CurrentProfile;
import net.devstudy.resume.ms.auth.application.port.in.security.CurrentProfileProvider;
import net.devstudy.resume.ms.auth.application.port.in.service.OidcAuthorizationRevocationService;
import net.devstudy.resume.ms.auth.application.port.in.service.ProfileAccountService;
import net.devstudy.resume.ms.auth.application.port.in.service.RestoreAccessService;
import net.devstudy.resume.ms.auth.application.port.in.service.UidSuggestionService;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.ms.auth.ports.profile.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.ms.auth.ports.profile.exception.UidAlreadyExistsException;
import net.devstudy.resume.ms.auth.application.support.component.DataBuilder;
import net.devstudy.resume.ms.auth.adapters.web.controller.api.PublicAuthApiController;
import net.devstudy.resume.ms.auth.adapters.web.security.RememberMeSupport;

@WebMvcTest(controllers = PublicAuthApiController.class, properties = {
        "app.security.session.enabled=true",
        "app.security.oidc.enabled=false",
        "app.auth.self-register.enabled=true",
        "app.auth.password-restore.enabled=true",
        "app.restore.show-link=true"
})
@AutoConfigureMockMvc(addFilters = false)
class PublicAuthApiControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PublicAuthApiController controller;

    @MockitoBean
    private ProfileAccountService profileAccountService;

    @MockitoBean
    private CurrentProfileProvider currentProfileProvider;

    @MockitoBean
    private UidSuggestionService uidSuggestionService;

    @MockitoBean
    private RestoreAccessService restoreAccessService;

    @MockitoBean
    private DataBuilder dataBuilder;

    @MockitoBean
    private RememberMeSupport rememberMeSupport;

    @MockitoBean
    private OidcAuthorizationRevocationService oidcAuthorizationRevocationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        ReflectionTestUtils.setField(controller, "selfRegisterEnabled", true);
    }

    @Test
    void registerShouldCreateSessionWhenRequestIsValid() throws Exception {
        when(profileAccountService.register(any(ProfileRegistrationRequest.class))).thenReturn(profile("john"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson("john", "John", "Doe")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.uid").value("john"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));

        verify(profileAccountService).register(any(ProfileRegistrationRequest.class));
        verify(rememberMeSupport).loginSuccess(any(), any(), any(), eq(false));
    }

    @Test
    void registerShouldReturnForbiddenWhenSelfRegistrationIsDisabled() throws Exception {
        ReflectionTestUtils.setField(controller, "selfRegisterEnabled", false);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson("john", "John", "Doe")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Self registration is disabled"));
    }

    @Test
    void registerShouldReturnConflictWhenAlreadyAuthenticated() throws Exception {
        when(currentProfileProvider.getCurrentProfile()).thenReturn(new CurrentProfile(10L, "john", "John Doe"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson("john", "John", "Doe")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Already authenticated"));
    }

    @Test
    void registerShouldReturnUidSuggestionsWhenUidAlreadyExists() throws Exception {
        when(profileAccountService.register(any(ProfileRegistrationRequest.class)))
                .thenThrow(new UidAlreadyExistsException("john"));
        when(uidSuggestionService.suggest("john")).thenReturn(List.of("john_1", "john_2"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson("john", "John", "Doe")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("Uid already exists: john"))
                .andExpect(jsonPath("$.uidSuggestions[0]").value("john_1"));
    }

    @Test
    void uidHintShouldReturnGeneratedUid() throws Exception {
        when(dataBuilder.buildProfileUid("John", "Doe")).thenReturn("john_doe");

        mockMvc.perform(get("/api/auth/uid-hint")
                        .param("firstName", "John")
                        .param("lastName", "Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("john_doe"));
    }

    @Test
    void restoreRequestShouldReturnBadRequestForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/auth/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void restoreStatusShouldReturnNotFoundWhenTokenIsInvalid() throws Exception {
        when(restoreAccessService.findProfileByToken("bad-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/restore/bad-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Restore token invalid"));
    }

    @Test
    void restorePasswordShouldReturnBadRequestWhenTokenIsInvalid() throws Exception {
        doThrow(new IllegalArgumentException("invalid token"))
                .when(restoreAccessService)
                .resetPassword("bad-token", "Secret123");

        mockMvc.perform(post("/api/auth/restore/bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restorePasswordJson("Secret123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Restore token invalid"));
    }

    @Test
    void logoutAllShouldReturnUnauthorizedWhenNoCurrentProfile() throws Exception {
        when(currentProfileProvider.getCurrentProfile()).thenReturn(null);

        mockMvc.perform(post("/api/auth/logout-all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void logoutAllShouldRevokeOidcAndRememberMeWhenAuthenticated() throws Exception {
        when(currentProfileProvider.getCurrentProfile()).thenReturn(new CurrentProfile(10L, "john", "John Doe"));

        mockMvc.perform(post("/api/auth/logout-all"))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Location"));

        verify(oidcAuthorizationRevocationService).revokeAllByPrincipal("john");
        verify(rememberMeSupport).logout(any(), any(), any());
    }

    private static ProfileAuthResponse profile(String uid) {
        return new ProfileAuthResponse(1L, uid, "hash", "John", "Doe", "john@example.com", "+123456789");
    }

    private static String registerRequestJson(String uid, String firstName, String lastName) {
        return """
                {
                  "uid": "%s",
                  "firstName": "%s",
                  "lastName": "%s",
                  "password": "Secret123",
                  "confirmPassword": "Secret123"
                }
                """.formatted(uid, firstName, lastName);
    }

    private static String restorePasswordJson(String password) {
        return """
                {
                  "password": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(password, password);
    }
}
