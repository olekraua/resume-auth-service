package net.devstudy.resume.ms.auth.application.security;

import org.springframework.security.authentication.LockedException;

public class LoginLockedException extends LockedException {

    private final long retryAfterSeconds;

    public LoginLockedException(long retryAfterSeconds) {
        super("Too many failed login attempts. Try again later.");
        this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
