package com.example.api.domain.user.exception;

/**
 * Thrown when attempting to register with an email that is already in use.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: %s".formatted(email));
    }
}
