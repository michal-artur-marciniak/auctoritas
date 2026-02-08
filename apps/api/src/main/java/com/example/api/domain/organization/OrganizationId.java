package com.example.api.domain.organization;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique organization identifier.
 */
public record OrganizationId(String value) {

    public OrganizationId {
        Objects.requireNonNull(value, "Organization ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Organization ID must not be blank");
        }
    }

    public static OrganizationId generate() {
        return new OrganizationId(UUID.randomUUID().toString());
    }

    public static OrganizationId of(String value) {
        return new OrganizationId(value);
    }
}
