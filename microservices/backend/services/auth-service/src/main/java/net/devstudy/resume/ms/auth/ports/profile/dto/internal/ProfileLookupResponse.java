package net.devstudy.resume.ms.auth.ports.profile.dto.internal;

public record ProfileLookupResponse(
        Long id,
        String uid,
        String email,
        String phone,
        String firstName,
        String lastName
) {
}
