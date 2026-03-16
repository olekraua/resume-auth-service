package net.devstudy.resume.web.security;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.security.jwt.enabled", havingValue = "true")
public class JwtAuthenticationFailureEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFailureEntryPoint.class);

    private final AuthenticationEntryPoint delegate = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        JwtErrorType jwtErrorType = classify(authException);
        if (jwtErrorType.alertable()) {
            LOGGER.error(
                    "jwt_validation_error=1 jwt_error_type={} method={} path={} "
                            + "oauth2_error_code={} error_class={} reason=\"{}\"",
                    jwtErrorType.code(),
                    request.getMethod(),
                    resolvePath(request),
                    resolveOAuth2ErrorCode(authException),
                    authException.getClass().getSimpleName(),
                    sanitize(authException.getMessage())
            );
        }
        delegate.commence(request, response, authException);
    }

    private JwtErrorType classify(AuthenticationException ex) {
        String allMessages = collectCauseMessages(ex).toLowerCase(Locale.ROOT);
        if (allMessages.contains("invalid signature")
                || allMessages.contains("bad jws signature")
                || allMessages.contains("jws verification failed")) {
            return JwtErrorType.INVALID_SIGNATURE;
        }
        if (allMessages.contains("no matching key(s) found")
                || allMessages.contains("no matching key found")
                || (allMessages.contains("kid") && allMessages.contains("not found"))) {
            return JwtErrorType.UNKNOWN_KID;
        }
        return JwtErrorType.OTHER;
    }

    private String collectCauseMessages(Throwable ex) {
        Set<String> parts = new LinkedHashSet<>();
        Throwable current = ex;
        int depth = 0;
        while (current != null && depth < 10) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                parts.add(current.getMessage());
            }
            current = current.getCause();
            depth++;
        }
        return String.join(" | ", parts);
    }

    private String resolveOAuth2ErrorCode(AuthenticationException ex) {
        if (ex instanceof OAuth2AuthenticationException oauth2AuthenticationException
                && oauth2AuthenticationException.getError() != null) {
            return oauth2AuthenticationException.getError().getErrorCode();
        }
        return "unauthorized";
    }

    private String resolvePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String normalized = message.replace('\n', ' ')
                .replace('\r', ' ')
                .replace('"', '\'')
                .trim();
        if (normalized.length() > 300) {
            return normalized.substring(0, 300);
        }
        return normalized;
    }

    private enum JwtErrorType {
        INVALID_SIGNATURE("invalid_signature", true),
        UNKNOWN_KID("unknown_kid", true),
        OTHER("other", false);

        private final String code;
        private final boolean alertable;

        JwtErrorType(String code, boolean alertable) {
            this.code = code;
            this.alertable = alertable;
        }

        public String code() {
            return code;
        }

        public boolean alertable() {
            return alertable;
        }
    }
}
