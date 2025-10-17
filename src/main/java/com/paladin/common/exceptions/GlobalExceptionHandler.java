package com.paladin.common.exceptions;

import com.paladin.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler({
            NotFoundException.class,
            ProfileNotFoundException.class,
            CVNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundExceptions(
            RuntimeException ex, WebRequest request) {
        log.warn("Not Found Exception: {} - {}", ex.getMessage(),
                request.getDescription(false));
        HttpStatus status = HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(
                new ErrorResponse(ex.getMessage(),
                        request.getDescription(false), status.value()),
                status
        );
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileException(
            InvalidFileException ex, WebRequest request) {
        log.warn("Invalid File Exception: {} - {}", ex.getMessage(),
                request.getDescription(false));
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(
                new ErrorResponse(ex.getMessage(),
                        request.getDescription(false), status.value()),
                status
        );
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmailException(
            DuplicateEmailException ex, WebRequest request) {
        log.warn("Duplicate Email Exception: {} - {}", ex.getMessage(),
                request.getDescription(false));
        HttpStatus status = HttpStatus.CONFLICT;
        return new ResponseEntity<>(
                new ErrorResponse(ex.getMessage(),
                        request.getDescription(false), status.value()),
                status
        );
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(
            UnauthorizedAccessException ex, WebRequest request) {
        log.warn("Unauthorized Access Exception: {} - {}", ex.getMessage(),
                request.getDescription(false));
        HttpStatus status = HttpStatus.FORBIDDEN;
        return new ResponseEntity<>(
                new ErrorResponse(ex.getMessage(),
                        request.getDescription(false), status.value()),
                status
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(
            UsernameNotFoundException ex, WebRequest request) {
        log.warn("Username Not Found Exception: {} - {}", ex.getMessage(),
                request.getDescription(false));
        HttpStatus status = HttpStatus.UNAUTHORIZED; // or NOT FOUND
        return new ResponseEntity<>(
                new ErrorResponse("Invalid credentials or user not found.",
                        request.getDescription(false), status.value()),
                status
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        log.warn("Bad Credentials Exception: {} - {}", ex.getMessage(),
                request.getDescription(false));
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return new ResponseEntity<>(
                new ErrorResponse("Invalid email or password.",
                        request.getDescription(false), status.value()),
                status
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation Exception: {} - {}", errors,
                request.getDescription(false));
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(
                new ErrorResponse("Validation failed: " + errors,
                        request.getDescription(false), status.value()),
                status
        );
    }

    @ExceptionHandler(CannotSendMailException.class)
    public ResponseEntity<ErrorResponse> handleNotImplementedException(
            Exception ex, WebRequest request) {
        log.error("Action Not Implemented Exception: {} - {}", ex.getMessage(),
                request.getDescription(false), ex);
        HttpStatus status = HttpStatus.NOT_IMPLEMENTED;
        return new ResponseEntity<>(
                new ErrorResponse(
                        "Action Not Implemented Exception. Please check your inputs and try again later.",
                        request.getDescription(false), status.value()),
                status
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("An unexpected error occurred: {} - {}", ex.getMessage(),
                request.getDescription(false), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(
                new ErrorResponse(
                        "An unexpected error occurred. Please try again later.",
                        request.getDescription(false), status.value()),
                status
        );
    }
}
