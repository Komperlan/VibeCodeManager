package com.aiq.adapters.web.common;

import com.aiq.domain.common.DomainException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String GLOBAL_ERROR_KEY = "_global";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Request validation failed",
            validationDetails(exception.getBindingResult())
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Request validation failed",
            constraintViolationDetails(exception)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST_BODY",
            "Request body is malformed or contains invalid values"
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String parameterName = exception.getName();
        String requiredType = exception.getRequiredType() == null
            ? "expected type"
            : exception.getRequiredType().getSimpleName();

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST_PARAMETER",
            "Request parameter has invalid type",
            Map.of(parameterName, List.of("must be a valid " + requiredType))
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
        MissingServletRequestParameterException exception
    ) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "MISSING_REQUEST_PARAMETER",
            "Required request parameter is missing",
            Map.of(exception.getParameterName(), List.of("must be present"))
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException exception) {
        return buildResponse(
            HttpStatus.NOT_FOUND,
            "NOT_FOUND",
            safeMessage(exception, "Resource not found")
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException exception) {
        return buildResponse(
            HttpStatus.NOT_FOUND,
            "NOT_FOUND",
            "API endpoint not found"
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
        return buildResponse(
            HttpStatus.NOT_FOUND,
            "NOT_FOUND",
            "API endpoint not found"
        );
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException exception) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "DOMAIN_ERROR",
            safeMessage(exception, "Domain rule violation")
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        if (isNotFound(exception)) {
            return buildResponse(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                safeMessage(exception, "Resource not found")
            );
        }

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "BAD_REQUEST",
            safeMessage(exception, "Invalid request")
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException exception) {
        return buildResponse(
            HttpStatus.CONFLICT,
            "CONFLICT",
            safeMessage(exception, "Operation conflicts with current resource state")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected API error", exception);
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "Unexpected server error"
        );
    }

    private static ResponseEntity<ErrorResponse> buildResponse(
        HttpStatus status,
        String code,
        String message
    ) {
        return buildResponse(status, code, message, Map.of());
    }

    private static ResponseEntity<ErrorResponse> buildResponse(
        HttpStatus status,
        String code,
        String message,
        Map<String, List<String>> details
    ) {
        return ResponseEntity
            .status(status)
            .body(ErrorResponse.of(code, message, details));
    }

    private static Map<String, List<String>> validationDetails(BindingResult bindingResult) {
        Map<String, List<String>> details = new LinkedHashMap<>();

        for (FieldError error : bindingResult.getFieldErrors()) {
            addDetail(details, error.getField(), safeMessage(error));
        }
        for (ObjectError error : bindingResult.getGlobalErrors()) {
            addDetail(details, GLOBAL_ERROR_KEY, safeMessage(error));
        }

        return details;
    }

    private static Map<String, List<String>> constraintViolationDetails(
        ConstraintViolationException exception
    ) {
        Map<String, List<String>> details = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            addDetail(
                details,
                violation.getPropertyPath().toString(),
                violation.getMessage()
            );
        }

        return details;
    }

    private static void addDetail(Map<String, List<String>> details, String key, String message) {
        details.computeIfAbsent(key, ignored -> new ArrayList<>()).add(message);
    }

    private static boolean isNotFound(IllegalArgumentException exception) {
        String message = exception.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("not found");
    }

    private static String safeMessage(RuntimeException exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
            ? fallback
            : exception.getMessage();
    }

    private static String safeMessage(ObjectError error) {
        return error.getDefaultMessage() == null || error.getDefaultMessage().isBlank()
            ? "Invalid value"
            : error.getDefaultMessage();
    }
}
