package com.example.api.domain.environment;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique environment identifier.
 */
public record EnvironmentId(String value) {

    public EnvironmentId {
        Objects.requireNonNull(value, "Environment ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Environment ID must not be blank");
        }
    }

    public static EnvironmentId generate() {
        return new EnvironmentId(UUID.randomUUID().toString());
    }

    public static EnvironmentId of(String value) {
        return new EnvironmentId(value);
    }
}
