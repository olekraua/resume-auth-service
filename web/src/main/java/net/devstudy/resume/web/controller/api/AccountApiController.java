package net.devstudy.resume.web.controller.api;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.api.dto.ChangeLoginForm;
import net.devstudy.resume.auth.api.dto.ChangePasswordForm;
import net.devstudy.resume.auth.api.model.CurrentProfile;
import net.devstudy.resume.auth.api.security.CurrentProfileProvider;
import net.devstudy.resume.auth.api.service.ProfileAccountService;
import net.devstudy.resume.auth.api.service.UidSuggestionService;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfilePasswordUpdateRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileUidUpdateRequest;
import net.devstudy.resume.profile.api.exception.UidAlreadyExistsException;
import net.devstudy.resume.shared.dto.ApiErrorResponse;
import net.devstudy.resume.web.security.RememberMeSupport;
import net.devstudy.resume.web.api.ApiErrorUtils;

@RestController
@RequestMapping("/api/account")
@ConditionalOnProperty(name = "app.security.session.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AccountApiController {

    private final PasswordEncoder passwordEncoder;
    private final CurrentProfileProvider currentProfileProvider;
    private final UidSuggestionService uidSuggestionService;
    private final RememberMeSupport rememberMeSupport;
    private final ProfileAccountService profileAccountService;

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordForm form,
            BindingResult bindingResult,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        CurrentProfile currentProfile = currentProfileProvider.getCurrentProfile();
        if (currentProfile == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        ProfileAuthResponse profile = profileAccountService.loadForAuth(currentProfile.getUsername());
        if (profile == null || profile.id() == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        if (!passwordEncoder.matches(form.getCurrentPassword(), profile.passwordHash())) {
            ApiErrorResponse error = ApiErrorResponse.of(
                    HttpStatus.BAD_REQUEST,
                    "Current password is invalid",
                    ApiErrorUtils.resolvePath(request),
                    List.of(new ApiErrorResponse.FieldError("currentPassword", "Невірний поточний пароль"))
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        profileAccountService.updatePassword(currentId, new ProfilePasswordUpdateRequest(form.getNewPassword()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> changeLogin(@Valid @RequestBody ChangeLoginForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        try {
            profileAccountService.updateUid(currentId, new ProfileUidUpdateRequest(form.getNewUid()));
        } catch (UidAlreadyExistsException ex) {
            List<String> suggestions = uidSuggestionService.suggest(ex.getUid());
            ApiErrorResponse error = ApiErrorResponse.of(
                    HttpStatus.CONFLICT,
                    ex.getMessage(),
                    ApiErrorUtils.resolvePath(request),
                    List.of(new ApiErrorResponse.FieldError("newUid", ex.getMessage()))
            );
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new AuthApiController.UidConflictResponse(error, suggestions));
        } catch (IllegalArgumentException ex) {
            return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        rememberMeSupport.logout(request, response, authentication);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.ok(new ChangeLoginResponse(form.getNewUid(), true));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeAccount(HttpServletRequest request, HttpServletResponse response) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        profileAccountService.removeProfile(currentId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        rememberMeSupport.logout(request, response, authentication);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    public record ChangeLoginResponse(String newUid, boolean reloginRequired) {
    }
}
