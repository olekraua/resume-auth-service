package net.devstudy.resume.web.controller.api;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.api.dto.RegistrationForm;
import net.devstudy.resume.auth.api.dto.RestoreAccessForm;
import net.devstudy.resume.auth.api.dto.RestorePasswordForm;
import net.devstudy.resume.auth.api.model.CurrentProfile;
import net.devstudy.resume.auth.api.security.CurrentProfileProvider;
import net.devstudy.resume.auth.api.service.ProfileAccountService;
import net.devstudy.resume.auth.api.service.RestoreAccessService;
import net.devstudy.resume.auth.api.service.UidSuggestionService;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.profile.api.exception.UidAlreadyExistsException;
import net.devstudy.resume.shared.component.DataBuilder;
import net.devstudy.resume.shared.dto.ApiErrorResponse;
import net.devstudy.resume.web.api.ApiErrorUtils;
import net.devstudy.resume.web.controller.SessionApiController;
import net.devstudy.resume.web.security.RememberMeSupport;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PublicAuthApiController {

    private final ProfileAccountService profileAccountService;
    private final ObjectProvider<CurrentProfileProvider> currentProfileProvider;
    private final UidSuggestionService uidSuggestionService;
    private final RestoreAccessService restoreAccessService;
    private final DataBuilder dataBuilder;
    private final ObjectProvider<RememberMeSupport> rememberMeSupportProvider;

    @Value("${app.security.session.enabled:true}")
    private boolean sessionEnabled;

    @Value("${app.security.oidc.enabled:false}")
    private boolean oidcEnabled;

    @Value("${app.auth.self-register.enabled:true}")
    private boolean selfRegisterEnabled;

    @Value("${app.auth.password-restore.enabled:true}")
    private boolean passwordRestoreEnabled;

    @Value("${app.restore.show-link:false}")
    private boolean showRestoreLink;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationForm form, BindingResult bindingResult,
            HttpServletRequest request, HttpServletResponse response) {
        if (!selfRegisterEnabled) {
            return ApiErrorUtils.error(HttpStatus.FORBIDDEN, "Self registration is disabled", request);
        }
        if (resolveCurrentProfile() != null) {
            return ApiErrorUtils.error(HttpStatus.CONFLICT, "Already authenticated", request);
        }
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        try {
            ProfileAuthResponse profile = profileAccountService.register(
                    new ProfileRegistrationRequest(form.getUid(), form.getFirstName(), form.getLastName(),
                            form.getPassword()));
            CurrentProfile currentProfile = new CurrentProfile(profile);
            boolean authenticated = establishSessionIfEnabled(currentProfile, request, response);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new SessionApiController.SessionResponse(
                            authenticated,
                            currentProfile.getUsername(),
                            currentProfile.getFullName()
                    ));
        } catch (UidAlreadyExistsException ex) {
            List<String> suggestions = uidSuggestionService.suggest(ex.getUid());
            ApiErrorResponse error = ApiErrorResponse.of(
                    HttpStatus.CONFLICT,
                    ex.getMessage(),
                    ApiErrorUtils.resolvePath(request),
                    List.of(new ApiErrorResponse.FieldError("uid", ex.getMessage()))
            );
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new UidConflictResponse(error, suggestions));
        } catch (IllegalArgumentException ex) {
            return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        }
    }

    @GetMapping("/uid-hint")
    public UidHintResponse uidHint(@RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        return new UidHintResponse(dataBuilder.buildProfileUid(firstName, lastName));
    }

    @PostMapping("/restore")
    public ResponseEntity<?> requestRestore(@Valid @RequestBody RestoreAccessForm form, BindingResult bindingResult,
            HttpServletRequest request) {
        if (!passwordRestoreEnabled) {
            return ApiErrorUtils.error(HttpStatus.FORBIDDEN, "Password restore is disabled", request);
        }
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        String appHost = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
        String link = null;
        try {
            link = restoreAccessService.requestRestore(form.getIdentifier(), appHost);
        } catch (IllegalArgumentException ex) {
            link = null;
        }
        if (!showRestoreLink) {
            link = null;
        }
        return ResponseEntity.ok(new RestoreRequestResponse(true, link));
    }

    @GetMapping("/restore/{token}")
    public ResponseEntity<?> restoreStatus(@PathVariable String token, HttpServletRequest request) {
        if (!passwordRestoreEnabled) {
            return ApiErrorUtils.error(HttpStatus.FORBIDDEN, "Password restore is disabled", request);
        }
        boolean valid = restoreAccessService.findProfileByToken(token).isPresent();
        if (!valid) {
            return ApiErrorUtils.error(HttpStatus.NOT_FOUND, "Restore token invalid", request);
        }
        return ResponseEntity.ok(new RestoreTokenResponse(true));
    }

    @PostMapping("/restore/{token}")
    public ResponseEntity<?> restorePassword(@PathVariable String token,
            @Valid @RequestBody RestorePasswordForm form,
            BindingResult bindingResult,
            HttpServletRequest request) {
        if (!passwordRestoreEnabled) {
            return ApiErrorUtils.error(HttpStatus.FORBIDDEN, "Password restore is disabled", request);
        }
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        try {
            restoreAccessService.resetPassword(token, form.getPassword());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, "Restore token invalid", request);
        }
    }

    private boolean establishSessionIfEnabled(CurrentProfile currentProfile,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (!isLocalPasswordAuthEnabled()) {
            return false;
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                currentProfile, null, currentProfile.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        RememberMeSupport rememberMeSupport = rememberMeSupportProvider.getIfAvailable();
        if (rememberMeSupport != null) {
            rememberMeSupport.loginSuccess(request, response, authentication, false);
        }
        return true;
    }

    private boolean isLocalPasswordAuthEnabled() {
        return sessionEnabled && !oidcEnabled;
    }

    private CurrentProfile resolveCurrentProfile() {
        CurrentProfileProvider provider = currentProfileProvider.getIfAvailable();
        if (provider == null) {
            return null;
        }
        return provider.getCurrentProfile();
    }

    public record UidHintResponse(String uid) {
    }

    public record RestoreRequestResponse(boolean requested, String restoreLink) {
    }

    public record RestoreTokenResponse(boolean valid) {
    }

    public record UidConflictResponse(ApiErrorResponse error, List<String> uidSuggestions) {
    }

}
