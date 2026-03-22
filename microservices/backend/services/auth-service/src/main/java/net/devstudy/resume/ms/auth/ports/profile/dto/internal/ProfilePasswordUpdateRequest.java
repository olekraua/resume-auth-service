package net.devstudy.resume.ms.auth.ports.profile.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfilePasswordUpdateRequest(
        @NotBlank @Size(min = 6, max = 128) String password
) {
}
