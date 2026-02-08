package com.example.api.infrastructure.stripe;

import com.stripe.model.Subscription;

import java.util.Map;

public record StripeSubscription(
        String id,
        String customer,
        Long currentPeriodEnd,
        String status,
        Map<String, String> metadata
) {
    public static StripeSubscription from(Subscription subscription) {
        if (subscription == null) {
            return null;
        }
        return new StripeSubscription(
                subscription.getId(),
                subscription.getCustomer(),
                subscription.getCurrentPeriodEnd(),
                subscription.getStatus(),
                subscription.getMetadata()
        );
    }
}
