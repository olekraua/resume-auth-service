package net.devstudy.resume.web.controller.api;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.api.model.CurrentProfile;
import net.devstudy.resume.auth.api.security.CurrentProfileProvider;
import net.devstudy.resume.shared.dto.ApiErrorResponse;
import net.devstudy.resume.web.controller.SessionApiController;
import net.devstudy.resume.web.api.ApiErrorUtils;
import net.devstudy.resume.web.security.RememberMeSupport;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "app.security.session.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AuthApiController {

    private final CurrentProfileProvider currentProfileProvider;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final RememberMeSupport rememberMeSupport;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, httpRequest);
        }
        CurrentProfile existing = currentProfileProvider.getCurrentProfile();
        if (existing != null) {
            return ResponseEntity.ok(toSessionResponse(existing));
        }
        AuthenticationManager authenticationManager;
        try {
            authenticationManager = authenticationConfiguration.getAuthenticationManager();
        } catch (Exception ex) {
            return ApiErrorUtils.error(HttpStatus.INTERNAL_SERVER_ERROR, "Authentication unavailable", httpRequest);
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException ex) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Invalid username or password", httpRequest);
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        rememberMeSupport.loginSuccess(httpRequest, httpResponse, authentication, request.rememberMe());
        CurrentProfile currentProfile = resolveCurrentProfile(authentication);
        return ResponseEntity.ok(toSessionResponse(currentProfile));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        rememberMeSupport.logout(request, response, authentication);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    private SessionApiController.SessionResponse toSessionResponse(CurrentProfile currentProfile) {
        if (currentProfile == null) {
            return new SessionApiController.SessionResponse(false, null, null);
        }
        return new SessionApiController.SessionResponse(
                true,
                currentProfile.getUsername(),
                currentProfile.getFullName()
        );
    }

    private CurrentProfile resolveCurrentProfile(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CurrentProfile currentProfile) {
            return currentProfile;
        }
        return currentProfileProvider.getCurrentProfile();
    }

    public record LoginRequest(
            @jakarta.validation.constraints.NotBlank String username,
            @jakarta.validation.constraints.NotBlank String password,
            boolean rememberMe
    ) {
    }

    public record UidConflictResponse(ApiErrorResponse error, List<String> uidSuggestions) {
    }
}
