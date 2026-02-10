package net.devstudy.resume.profile.internal.web;

import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileIdentifierLookupRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileLookupResponse;
import net.devstudy.resume.profile.api.dto.internal.ProfileRegistrationRequest;
import net.devstudy.resume.profile.api.dto.internal.ProfileUidUpdateRequest;
import net.devstudy.resume.profile.api.exception.UidAlreadyExistsException;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.service.ProfileReadService;
import net.devstudy.resume.profile.api.service.ProfileService;

@RestController
@RequestMapping("/internal/profiles")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.internal.api.enabled", havingValue = "true")
public class ProfileInternalApiController {

    private final ProfileService profileService;
    private final ProfileReadService profileReadService;

    @PostMapping
    public ProfileAuthResponse register(@Valid @RequestBody ProfileRegistrationRequest request) {
        try {
            Profile profile = profileService.register(request.uid(), request.firstName(), request.lastName(),
                    request.password());
            return toAuth(profile);
        } catch (UidAlreadyExistsException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/exists/uid/{uid}")
    public boolean uidExists(@PathVariable String uid) {
        return profileReadService.uidExists(normalize(uid));
    }

    @PostMapping("/lookup")
    public ProfileLookupResponse lookup(@Valid @RequestBody ProfileIdentifierLookupRequest request) {
        String identifier = request.identifier();
        if (identifier == null || identifier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifier is required");
        }
        String trimmed = identifier.trim();
        String lower = trimmed.toLowerCase(Locale.ENGLISH);
        Profile profile = profileReadService.findByUid(lower)
                .or(() -> profileReadService.findByEmail(lower))
                .or(() -> profileReadService.findByPhone(trimmed))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        return toLookup(profile);
    }

    @PutMapping("/{id}/uid")
    public ResponseEntity<Void> updateUid(@PathVariable Long id,
            @Valid @RequestBody ProfileUidUpdateRequest request) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile id required");
        }
        try {
            profileService.updateUid(id, request.uid());
        } catch (UidAlreadyExistsException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeProfile(@PathVariable Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile id required");
        }
        profileService.removeProfile(id);
        return ResponseEntity.noContent().build();
    }

    private String normalize(String uid) {
        return uid == null ? null : uid.trim().toLowerCase(Locale.ENGLISH);
    }

    private ProfileAuthResponse toAuth(Profile profile) {
        return new ProfileAuthResponse(
                profile.getId(),
                profile.getUid(),
                null,
                profile.getFirstName(),
                profile.getLastName(),
                profile.getEmail(),
                profile.getPhone()
        );
    }

    private ProfileLookupResponse toLookup(Profile profile) {
        return new ProfileLookupResponse(
                profile.getId(),
                profile.getUid(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getFirstName(),
                profile.getLastName()
        );
    }
}
