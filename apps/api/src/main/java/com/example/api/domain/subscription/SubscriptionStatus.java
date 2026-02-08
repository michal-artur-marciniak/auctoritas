package com.example.api.domain.subscription;

/**
 * Subscription lifecycle state.
 */
public enum SubscriptionStatus {
    TRIALING,
    ACTIVE,
    PAST_DUE,
    CANCELED
}
