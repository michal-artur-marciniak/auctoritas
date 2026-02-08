package com.example.api.domain.session.exception;

/**
 * Thrown when a session cannot be found.
 */
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String sessionId) {
        super("Session not found: " + sessionId);
    }
}
