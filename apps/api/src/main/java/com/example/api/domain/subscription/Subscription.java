package com.example.api.domain.subscription;

import com.example.api.domain.user.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a user subscription.
 */
public class Subscription {

    private final SubscriptionId id;
    private final UserId userId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private Instant currentPeriodEnd;

    public Subscription(SubscriptionId id,
                        UserId userId,
                        SubscriptionPlan plan,
                        SubscriptionStatus status,
                        Instant currentPeriodEnd) {
        this.id = Objects.requireNonNull(id, "Subscription ID required");
        this.userId = Objects.requireNonNull(userId, "User ID required");
        this.plan = Objects.requireNonNull(plan, "Plan required");
        this.status = Objects.requireNonNull(status, "Status required");
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public static Subscription start(UserId userId, SubscriptionPlan plan) {
        return new Subscription(
                SubscriptionId.generate(),
                userId,
                plan,
                SubscriptionStatus.ACTIVE,
                null
        );
    }

    public void updateStatus(SubscriptionStatus status, Instant currentPeriodEnd) {
        this.status = Objects.requireNonNull(status, "Status required");
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public SubscriptionId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }
}
