package net.devstudy.resume.web.controller.api;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import net.devstudy.resume.shared.dto.ApiErrorResponse;
import net.devstudy.resume.web.api.ApiErrorUtils;

@ControllerAdvice(basePackages = "net.devstudy.resume.web.controller.api")
@ResponseBody
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        return ApiErrorUtils.badRequest(ex.getBindingResult(), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        return ApiErrorUtils.badRequest(ex.getBindingResult(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> errors = new ArrayList<>();
        if (ex != null) {
            for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
                String field = violation == null || violation.getPropertyPath() == null
                        ? ""
                        : violation.getPropertyPath().toString();
                String message = violation == null || violation.getMessage() == null ? "" : violation.getMessage();
                errors.add(new ApiErrorResponse.FieldError(field, message));
            }
        }
        return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(EntityNotFoundException ex,
            HttpServletRequest request) {
        return ApiErrorUtils.error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request) {
        return ApiErrorUtils.error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex,
            HttpServletRequest request) {
        return ApiErrorUtils.error(HttpStatus.CONFLICT, ex.getMessage(), request);
    }
}
