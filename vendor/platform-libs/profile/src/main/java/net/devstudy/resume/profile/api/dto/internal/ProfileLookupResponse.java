package net.devstudy.resume.profile.api.dto.internal;

public record ProfileLookupResponse(
        Long id,
        String uid,
        String email,
        String phone,
        String firstName,
        String lastName
) {
}
