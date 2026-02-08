package com.example.api.domain.user;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique user identifier.
 */
public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "User ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("User ID must not be blank");
        }
    }

    /**
     * Generates a new random user ID.
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }

    /**
     * Creates a UserId from an existing string value.
     */
    public static UserId of(String value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
