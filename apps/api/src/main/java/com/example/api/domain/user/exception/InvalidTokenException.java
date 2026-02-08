package com.example.api.domain.user.exception;

/**
 * Thrown when a token is invalid or expired.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Invalid or expired token");
    }
}
