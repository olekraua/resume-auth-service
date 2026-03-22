package net.devstudy.resume.ms.auth.ports.profile.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record ProfileIdentifierLookupRequest(@NotBlank String identifier) {
}
