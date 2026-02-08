package com.example.api.domain.project;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique project identifier.
 */
public record ProjectId(String value) {

    public ProjectId {
        Objects.requireNonNull(value, "Project ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Project ID must not be blank");
        }
    }

    public static ProjectId generate() {
        return new ProjectId(UUID.randomUUID().toString());
    }

    public static ProjectId of(String value) {
        return new ProjectId(value);
    }
}
