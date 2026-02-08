package com.example.api.domain.apikey;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique API key identifier.
 */
public record ApiKeyId(String value) {

    public ApiKeyId {
        Objects.requireNonNull(value, "API Key ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("API Key ID must not be blank");
        }
    }

    public static ApiKeyId generate() {
        return new ApiKeyId(UUID.randomUUID().toString());
    }

    public static ApiKeyId of(String value) {
        return new ApiKeyId(value);
    }
}
