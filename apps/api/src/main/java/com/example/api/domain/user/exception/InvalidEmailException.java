package com.example.api.domain.user.exception;

/**
 * Thrown when an email address fails validation.
 */
public class InvalidEmailException extends RuntimeException {

    public InvalidEmailException(String email) {
        super("Invalid email address: %s".formatted(email));
    }
}
