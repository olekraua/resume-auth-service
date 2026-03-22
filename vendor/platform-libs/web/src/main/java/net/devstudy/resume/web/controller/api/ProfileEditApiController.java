package net.devstudy.resume.web.controller.api;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.api.service.ProfileAccountService;
import net.devstudy.resume.auth.api.dto.ChangePasswordForm;
import net.devstudy.resume.auth.api.security.CurrentProfileProvider;
import net.devstudy.resume.media.api.dto.UploadCertificateResult;
import net.devstudy.resume.media.api.service.CertificateStorageService;
import net.devstudy.resume.media.api.service.PhotoStorageService;
import net.devstudy.resume.profile.api.dto.CertificateForm;
import net.devstudy.resume.profile.api.dto.ConnectionsVisibilityForm;
import net.devstudy.resume.profile.api.dto.ContactsForm;
import net.devstudy.resume.profile.api.dto.CourseForm;
import net.devstudy.resume.profile.api.dto.EducationForm;
import net.devstudy.resume.profile.api.dto.HobbyForm;
import net.devstudy.resume.profile.api.dto.InfoForm;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfilePasswordUpdateRequest;
import net.devstudy.resume.profile.api.dto.LanguageForm;
import net.devstudy.resume.profile.api.dto.PracticForm;
import net.devstudy.resume.profile.api.dto.SkillForm;
import net.devstudy.resume.profile.api.model.Certificate;
import net.devstudy.resume.profile.api.model.Education;
import net.devstudy.resume.profile.api.model.Practic;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.service.EditProfileService;
import net.devstudy.resume.profile.api.service.ProfileService;
import net.devstudy.resume.shared.dto.ApiErrorResponse;
import net.devstudy.resume.web.api.ApiErrorUtils;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileEditApiController {

    private final ProfileService profileService;
    private final EditProfileService editProfileService;
    private final CurrentProfileProvider currentProfileProvider;
    private final ObjectProvider<ProfileAccountService> profileAccountServiceProvider;
    private final PhotoStorageService photoStorageService;
    private final CertificateStorageService certificateStorageService;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    @PutMapping("/info")
    public ResponseEntity<?> updateInfo(@Valid @RequestBody InfoForm form, BindingResult bindingResult,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        profileService.updateInfo(currentId, form);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/contacts")
    public ResponseEntity<?> updateContacts(@Valid @RequestBody ContactsForm form, BindingResult bindingResult,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        profileService.updateContacts(currentId, form);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/skills")
    public ResponseEntity<?> updateSkills(@Valid @RequestBody SkillForm form, BindingResult bindingResult,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        profileService.updateSkills(currentId, form.getItems());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/practics")
    public ResponseEntity<?> updatePractics(@RequestBody PracticForm form, HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        List<Practic> filtered = editProfileService.preparePractics(form, bindingResult);
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        profileService.updatePractics(currentId, filtered);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/education")
    public ResponseEntity<?> updateEducation(@RequestBody EducationForm form, HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        List<Education> filtered = editProfileService.prepareEducations(form, bindingResult);
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        profileService.updateEducations(currentId, filtered);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/courses")
    public ResponseEntity<?> updateCourses(@Valid @RequestBody CourseForm form, BindingResult bindingResult,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        profileService.updateCourses(currentId, form.getItems());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/languages")
    public ResponseEntity<?> updateLanguages(@Valid @RequestBody LanguageForm form, BindingResult bindingResult,
            HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        editProfileService.addDuplicateLanguageErrors(form, bindingResult);
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        try {
            profileService.updateLanguages(currentId, form.getItems());
        } catch (DataIntegrityViolationException ex) {
            Locale locale = LocaleContextHolder.getLocale();
            String message = messageSource.getMessage(
                    "language.duplicate",
                    null,
                    "Language with the same name and type already exists.",
                    locale
            );
            ApiErrorResponse error = ApiErrorResponse.of(
                    HttpStatus.CONFLICT,
                    message,
                    ApiErrorUtils.resolvePath(request),
                    List.of(new ApiErrorResponse.FieldError("items", message))
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/certificates")
    public ResponseEntity<?> updateCertificates(@RequestBody CertificateForm form, HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        List<Certificate> filtered = editProfileService.prepareCertificates(form, bindingResult);
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        profileService.updateCertificates(currentId, filtered);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/hobbies")
    public ResponseEntity<?> updateHobbies(@Valid @RequestBody HobbyForm form, BindingResult bindingResult,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        profileService.updateHobbies(currentId, form.getHobbyIds());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@Valid @RequestBody ChangePasswordForm form,
            BindingResult bindingResult,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        String currentUid = currentProfileProvider.getCurrentProfile() == null
                ? null
                : currentProfileProvider.getCurrentProfile().getUsername();
        ProfileAccountService profileAccountService = profileAccountServiceProvider.getIfAvailable();
        if (profileAccountService == null) {
            return ApiErrorUtils.error(HttpStatus.NOT_IMPLEMENTED,
                    "Password change is handled by auth-service in OIDC mode",
                    request);
        }
        ProfileAuthResponse auth = profileAccountService.loadForAuth(currentUid);
        if (auth == null || auth.id() == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        if (!passwordEncoder.matches(form.getCurrentPassword(), auth.passwordHash())) {
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

    @GetMapping("/privacy/connections")
    public ResponseEntity<?> getConnectionsVisibility(HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        Optional<Profile> profileOpt = profileService.findById(currentId);
        if (profileOpt.isEmpty()) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        return ResponseEntity.ok(new ConnectionsVisibilityResponse(
                profileOpt.get().isConnectionsVisibleToConnections()
        ));
    }

    @PutMapping("/privacy/connections")
    public ResponseEntity<?> updateConnectionsVisibility(@Valid @RequestBody ConnectionsVisibilityForm form,
            BindingResult bindingResult,
            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return ApiErrorUtils.badRequest(bindingResult, request);
        }
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        profileService.updateConnectionsVisibility(currentId, form.getVisibleToConnections());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/photo")
    public ResponseEntity<?> uploadPhoto(@RequestParam("profilePhoto") MultipartFile profilePhoto,
            HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        if (profilePhoto == null || profilePhoto.isEmpty()) {
            return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, "Photo is required", request);
        }
        try {
            String[] urls = photoStorageService.store(profilePhoto);
            profileService.updatePhoto(currentId, urls[0], urls[1]);
            return ResponseEntity.ok(new PhotoResponse(urls[0], urls[1]));
        } catch (IllegalArgumentException ex) {
            return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        } catch (Exception ex) {
            return ApiErrorUtils.error(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store photo", request);
        }
    }

    @DeleteMapping("/photo")
    public ResponseEntity<?> removePhoto(HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        profileService.removePhoto(currentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/certificates/upload")
    public ResponseEntity<?> uploadCertificate(@RequestParam("certificateFile") MultipartFile certificateFile,
            HttpServletRequest request) {
        Long currentId = currentProfileProvider.getCurrentId();
        if (currentId == null) {
            return ApiErrorUtils.error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
        }
        if (certificateFile == null || certificateFile.isEmpty()) {
            return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, "Certificate file is required", request);
        }
        try {
            UploadCertificateResult result = certificateStorageService.store(certificateFile);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        } catch (Exception ex) {
            return ApiErrorUtils.error(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store certificate", request);
        }
    }

    public record PhotoResponse(String largeUrl, String smallUrl) {
    }

    public record ConnectionsVisibilityResponse(boolean visibleToConnections) {
    }
}
