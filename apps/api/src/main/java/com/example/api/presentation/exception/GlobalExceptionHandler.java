package com.example.api.presentation.exception;

import com.example.api.domain.session.exception.SessionNotFoundException;
import com.example.api.domain.user.exception.EmailAlreadyExistsException;
import com.example.api.domain.user.exception.InvalidCredentialsException;
import com.example.api.domain.user.exception.InvalidEmailException;
import com.example.api.domain.user.exception.UserBannedException;
import com.example.api.domain.user.exception.UserNotFoundException;
import com.example.api.domain.organization.exception.OrganizationSlugAlreadyExistsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler that maps domain exceptions to HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(401, ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(409, ex.getMessage()));
    }

    @ExceptionHandler(OrganizationSlugAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleOrganizationSlugConflict(OrganizationSlugAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(409, ex.getMessage()));
    }

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ApiError> handleInvalidEmail(InvalidEmailException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, ex.getMessage()));
    }

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<ApiError> handleOAuthError(
            OAuth2AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(401, ex.getMessage()));
    }

    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<ApiError> handleUserBanned(UserBannedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiError(403, ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiError> handleSessionNotFound(SessionNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        final var message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "Internal server error"));
    }
}
