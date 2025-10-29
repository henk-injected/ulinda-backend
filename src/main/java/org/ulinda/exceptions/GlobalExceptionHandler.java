package org.ulinda.exceptions;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.ulinda.services.ErrorService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Autowired
    private ErrorService errorService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Global Exception Handler: " + e.getMessage(), e);
        ErrorResponse error = new ErrorResponse();
        error.setMessage("Internal Server Error");
        error.setErrorCode(ErrorCode.INTERNAL_SERVER_ERROR);
        error.setTimestamp(Instant.now());
        error.setErrorIdentifier(UUID.randomUUID());
        saveErrorToDatabase(e, error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(FrontendException.class)
    public ResponseEntity<ErrorResponse> handleFrontendException(FrontendException e) {
        // Log the full stack trace including the cause
        log.warn("Frontend exception occurred: {}", e.getMessage(), e);

        ErrorResponse error = new ErrorResponse();
        error.setMessage(e.getMessage());
        error.setShowMessageToUser(e.isShowMessageToUser());
        if (e.getErrorCode() != null) {
            error.setErrorCode(e.getErrorCode());
        } else {
            error.setErrorCode(ErrorCode.GENERAL_ERROR);
        }
        error.setTimestamp(Instant.now());
        error.setErrorIdentifier(UUID.randomUUID());
        saveErrorToDatabase(e, error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation exception: ", e);

        // Extract field errors
        Map<String, String> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing // Keep first error if multiple for same field
                ));

        // Create validation error response
        ValidationErrorResponse error = new ValidationErrorResponse();
        error.setMessage("Validation failed");
        error.setErrorCode(ErrorCode.CONTROLLER_VALIDATION_EXCEPTION);
        error.setTimestamp(Instant.now());
        error.setErrorIdentifier(UUID.randomUUID());
        error.setFieldErrors(fieldErrors);
        error.setShowMessageToUser(true);
        saveErrorToDatabase(e, error);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e) {
        log.warn("Validation exception: ", e);

        ErrorResponse error = new ErrorResponse();
        error.setMessage(e.getMessage());
        error.setErrorCode(ErrorCode.VALIDATION_EXCEPTION);
        error.setTimestamp(Instant.now());
        error.setErrorIdentifier(UUID.randomUUID());
        error.setShowMessageToUser(true);
        saveErrorToDatabase(e, error);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    private void saveErrorToDatabase(Exception e, ErrorResponse er) {
        errorService.saveErrorAsync(e, er);
    }
}