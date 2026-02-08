package com.example.api.domain.session;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique session identifier.
 */
public record SessionId(String value) {

    public SessionId {
        Objects.requireNonNull(value, "Session ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Session ID must not be blank");
        }
    }

    /**
     * Generates a new random session ID.
     */
    public static SessionId generate() {
        return new SessionId(UUID.randomUUID().toString());
    }

    /**
     * Creates a SessionId from an existing value.
     */
    public static SessionId of(String value) {
        return new SessionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
