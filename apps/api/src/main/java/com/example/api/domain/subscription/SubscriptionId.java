package com.example.api.domain.subscription;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique subscription identifier.
 */
public record SubscriptionId(String value) {

    public SubscriptionId {
        Objects.requireNonNull(value, "Subscription ID must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Subscription ID must not be blank");
        }
    }

    public static SubscriptionId generate() {
        return new SubscriptionId(UUID.randomUUID().toString());
    }

    public static SubscriptionId of(String value) {
        return new SubscriptionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
