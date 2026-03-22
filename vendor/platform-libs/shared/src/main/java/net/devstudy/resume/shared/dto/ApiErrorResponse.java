package net.devstudy.resume.shared.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldError> errors
) {
    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, null);
    }

    public static ApiErrorResponse of(HttpStatus status, String message, String path, List<FieldError> errors) {
        String resolvedMessage = resolveValue(message, status.getReasonPhrase());
        String resolvedPath = path == null ? "" : path;
        List<FieldError> normalizedErrors = errors == null || errors.isEmpty() ? null : List.copyOf(errors);
        return new ApiErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                resolvedMessage,
                resolvedPath,
                normalizedErrors
        );
    }

    private static String resolveValue(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    public record FieldError(String field, String message) {
    }
}
