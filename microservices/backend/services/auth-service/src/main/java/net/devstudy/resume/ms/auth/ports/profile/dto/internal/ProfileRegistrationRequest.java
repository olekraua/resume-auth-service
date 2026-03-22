package net.devstudy.resume.ms.auth.ports.profile.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileRegistrationRequest(
        @NotBlank @Size(min = 3, max = 64) String uid,
        @NotBlank @Size(min = 1, max = 64) String firstName,
        @NotBlank @Size(min = 1, max = 64) String lastName,
        @NotBlank @Size(min = 6, max = 128) String password
) {
}
