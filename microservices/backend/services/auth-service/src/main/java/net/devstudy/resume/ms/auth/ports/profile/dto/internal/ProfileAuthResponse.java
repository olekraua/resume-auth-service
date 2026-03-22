package net.devstudy.resume.ms.auth.ports.profile.dto.internal;

public record ProfileAuthResponse(
        Long id,
        String uid,
        String passwordHash,
        String firstName,
        String lastName,
        String email,
        String phone
) {
}
