package net.devstudy.resume.profile.api.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUidUpdateRequest(
        @NotBlank @Size(min = 3, max = 64) String uid
) {
}
