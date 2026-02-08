package com.example.api.domain.user.exception;

/**
 * Thrown when a user is not found.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String identifier) {
        super("User not found: %s".formatted(identifier));
    }
}
