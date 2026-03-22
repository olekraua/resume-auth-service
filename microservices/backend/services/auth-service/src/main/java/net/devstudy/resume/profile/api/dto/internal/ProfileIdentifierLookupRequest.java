package net.devstudy.resume.profile.api.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record ProfileIdentifierLookupRequest(@NotBlank String identifier) {
}
