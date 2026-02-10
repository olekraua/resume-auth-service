package net.devstudy.resume.auth.api.service;

import java.util.Optional;

public interface RestoreAccessService {

    String requestRestore(String identifier, String appHost);

    Optional<Long> findProfileByToken(String token);

    void resetPassword(String token, String rawPassword);
}
