package com.example.event_ticketing.exception;

import com.example.event_ticketing.dto.ApiErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Turns every exception thrown by a controller into an {@link ApiErrorDTO} with a matching
 * HTTP status, so clients always receive the same JSON error shape.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({ValidationException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiErrorDTO> handleValidation(RuntimeException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Bean Validation failures on an {@code @Valid} body: every broken field's message, in field order. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Request is invalid" : message, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorDTO> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ApiErrorDTO> handleSeatUnavailable(SeatUnavailableException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Two customers raced for the same seat row; the loser is told to retry rather than seeing a 500. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorDTO> handleConcurrentUpdate(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "This item was changed by someone else. Please refresh and try again.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorDTO> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorDTO> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    /** Anything unexpected: log the detail server-side, never leak it to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.", request);
    }

    private static ResponseEntity<ApiErrorDTO> error(HttpStatus status, String message, HttpServletRequest request) {
        ApiErrorDTO body = new ApiErrorDTO(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
