package vn.amela.employeeservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import vn.amela.employeeservice.dto.response.ErrorResponse;
import vn.amela.employeeservice.dto.response.FieldErrorResponse;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmployeeException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeException(
        EmployeeException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            exception.getStatus().value(),
            exception.getCode(),
            exception.getMessage(),
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<FieldErrorResponse> errors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .collect(Collectors.toMap(
                FieldError::getField,
                this::fieldErrorMessage,
                (first, ignored) -> first,
                LinkedHashMap::new
            ))
            .entrySet()
            .stream()
            .map(entry -> new FieldErrorResponse(entry.getKey(), entry.getValue()))
            .toList();

        return buildResponse(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "Request is invalid",
            request.getRequestURI(),
            errors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        log.warn("Unreadable request body on {}: {}", request.getRequestURI(), exception.getMessage());
        return buildResponse(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_REQUEST_BODY",
            "Request body is missing or invalid",
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(
        DuplicateKeyException exception,
        HttpServletRequest request
    ) {
        log.warn("Duplicate key violation on {}: {}", request.getRequestURI(), exception.getMessage());
        return buildResponse(
            HttpStatus.CONFLICT.value(),
            "DUPLICATE_RESOURCE",
            "Resource already exists",
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
        DataIntegrityViolationException exception,
        HttpServletRequest request
    ) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), exception.getMessage());
        return buildResponse(
            HttpStatus.CONFLICT.value(),
            "CONSTRAINT_VIOLATION",
            "Data integrity constraint violated",
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
        ResponseStatusException exception,
        HttpServletRequest request
    ) {
        HttpStatus resolved = HttpStatus.resolve(exception.getStatusCode().value());
        int statusCode = resolved == null ? exception.getStatusCode().value() : resolved.value();
        return buildResponse(
            statusCode,
            resolved == null ? String.valueOf(statusCode) : resolved.name(),
            exception.getReason() == null
                ? (resolved == null ? "HTTP " + statusCode : resolved.getReasonPhrase())
                : exception.getReason(),
            request.getRequestURI(),
            List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        log.error("Unexpected error on {}", request.getRequestURI(), exception);
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            request.getRequestURI(),
            List.of()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorResponse> errors
    ) {
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(status)
            .code(code)
            .message(message)
            .path(path)
            .errors(errors)
            .build();

        return ResponseEntity.status(status).body(response);
    }

    private String fieldErrorMessage(FieldError error) {
        return error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage();
    }
}
