package com.example.api.domain.subscription;

import com.example.api.domain.user.UserId;

import java.util.Optional;

/**
 * Repository port for subscription persistence.
 */
public interface SubscriptionRepository {

    Optional<Subscription> findById(SubscriptionId id);

    Optional<Subscription> findByUserId(UserId userId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Subscription save(Subscription subscription);

    void delete(SubscriptionId id);
}
