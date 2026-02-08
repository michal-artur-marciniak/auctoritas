package com.example.api.domain.organization;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique invitation identifier.
 */
public record OrganizationInvitationId(String value) {

    public OrganizationInvitationId {
        Objects.requireNonNull(value, "Invitation ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Invitation ID must not be blank");
        }
    }

    public static OrganizationInvitationId generate() {
        return new OrganizationInvitationId(UUID.randomUUID().toString());
    }

    public static OrganizationInvitationId of(String value) {
        return new OrganizationInvitationId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
