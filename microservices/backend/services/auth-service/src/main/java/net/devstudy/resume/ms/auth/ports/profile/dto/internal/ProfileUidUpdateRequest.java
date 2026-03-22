package net.devstudy.resume.ms.auth.ports.profile.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUidUpdateRequest(
        @NotBlank @Size(min = 3, max = 64) String uid
) {
}
