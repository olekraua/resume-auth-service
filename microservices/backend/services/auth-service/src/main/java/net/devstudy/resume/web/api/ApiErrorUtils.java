package net.devstudy.resume.web.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import net.devstudy.resume.shared.dto.ApiErrorResponse;

public final class ApiErrorUtils {

    private ApiErrorUtils() {
    }

    public static ResponseEntity<ApiErrorResponse> badRequest(BindingResult bindingResult,
            HttpServletRequest request) {
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                resolvePath(request),
                toFieldErrors(bindingResult)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    public static ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message,
            HttpServletRequest request) {
        ApiErrorResponse error = ApiErrorResponse.of(status, message, resolvePath(request));
        return ResponseEntity.status(status).body(error);
    }

    public static ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message,
            HttpServletRequest request, List<ApiErrorResponse.FieldError> errors) {
        ApiErrorResponse error = ApiErrorResponse.of(status, message, resolvePath(request), errors);
        return ResponseEntity.status(status).body(error);
    }

    public static List<ApiErrorResponse.FieldError> toFieldErrors(BindingResult bindingResult) {
        if (bindingResult == null || !bindingResult.hasErrors()) {
            return List.of();
        }
        List<ApiErrorResponse.FieldError> errors = new ArrayList<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.add(new ApiErrorResponse.FieldError(fieldError.getField(), safeMessage(fieldError)));
        }
        for (ObjectError error : bindingResult.getGlobalErrors()) {
            errors.add(new ApiErrorResponse.FieldError(error.getObjectName(), safeMessage(error)));
        }
        return errors;
    }

    public static String resolvePath(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    private static String safeMessage(ObjectError error) {
        if (error == null || error.getDefaultMessage() == null) {
            return "";
        }
        return error.getDefaultMessage().trim();
    }
}
