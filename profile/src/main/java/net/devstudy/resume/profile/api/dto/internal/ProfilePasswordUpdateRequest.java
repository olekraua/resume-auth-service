package net.devstudy.resume.profile.api.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfilePasswordUpdateRequest(
        @NotBlank @Size(min = 6, max = 128) String password
) {
}
