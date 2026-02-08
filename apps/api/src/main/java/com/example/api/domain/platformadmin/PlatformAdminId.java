package com.example.api.domain.platformadmin;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique platform admin identifier.
 */
public record PlatformAdminId(String value) {

    public PlatformAdminId {
        Objects.requireNonNull(value, "Platform admin ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Platform admin ID must not be blank");
        }
    }

    public static PlatformAdminId generate() {
        return new PlatformAdminId(UUID.randomUUID().toString());
    }

    public static PlatformAdminId of(String value) {
        return new PlatformAdminId(value);
    }
}
