package com.example.api.domain.organization;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique organization member identifier.
 */
public record OrganizationMemberId(String value) {

    public OrganizationMemberId {
        Objects.requireNonNull(value, "Organization member ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Organization member ID must not be blank");
        }
    }

    public static OrganizationMemberId generate() {
        return new OrganizationMemberId(UUID.randomUUID().toString());
    }

    public static OrganizationMemberId of(String value) {
        return new OrganizationMemberId(value);
    }
}
