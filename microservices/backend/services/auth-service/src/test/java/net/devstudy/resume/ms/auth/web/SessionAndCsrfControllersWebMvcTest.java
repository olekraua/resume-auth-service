package net.devstudy.resume.ms.auth.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import net.devstudy.resume.auth.api.model.CurrentProfile;
import net.devstudy.resume.auth.api.security.CurrentProfileProvider;
import net.devstudy.resume.web.controller.SessionApiController;
import net.devstudy.resume.web.controller.api.CsrfApiController;

@WebMvcTest(controllers = {SessionApiController.class, CsrfApiController.class})
@AutoConfigureMockMvc(addFilters = false)
class SessionAndCsrfControllersWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentProfileProvider currentProfileProvider;

    @Test
    void sessionShouldReturnUnauthenticatedWhenNoCurrentProfile() throws Exception {
        when(currentProfileProvider.getCurrentProfile()).thenReturn(null);

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.uid").doesNotExist())
                .andExpect(jsonPath("$.fullName").doesNotExist());
    }

    @Test
    void sessionShouldReturnCurrentProfileWhenAuthenticated() throws Exception {
        when(currentProfileProvider.getCurrentProfile()).thenReturn(new CurrentProfile(1L, "john", "John Doe"));

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.uid").value("john"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    void csrfShouldReturnEmptyFieldsWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value(""))
                .andExpect(jsonPath("$.parameterName").value(""))
                .andExpect(jsonPath("$.token").value(""));
    }

    @Test
    void csrfShouldReturnTokenFieldsWhenTokenExists() throws Exception {
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "csrf-token-123");

        mockMvc.perform(get("/api/csrf")
                        .requestAttr(CsrfToken.class.getName(), token)
                        .requestAttr("_csrf", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.token").value("csrf-token-123"));
    }
}
