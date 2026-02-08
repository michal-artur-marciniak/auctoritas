package com.example.api.infrastructure.stripe;

import com.example.api.domain.subscription.Subscription;
import com.example.api.domain.subscription.SubscriptionPlan;
import com.example.api.domain.subscription.SubscriptionRepository;
import com.example.api.domain.subscription.SubscriptionStatus;
import com.example.api.domain.user.UserId;
import com.example.api.domain.user.UserRepository;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Handles Stripe subscription events and updates domain persistence.
 */
@Component
public class StripeSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(StripeSubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public StripeSubscriptionService(SubscriptionRepository subscriptionRepository,
                                     UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }

    public void handleCheckoutCompleted(Session session) {
        final var userId = extractUserId(session);
        final var stripeCustomerId = session.getCustomer();
        final var plan = extractPlan(session);

        if (userId == null || plan == null) {
            log.warn("Checkout session missing user metadata or plan: {}", session.getId());
            return;
        }

        if (stripeCustomerId != null && !stripeCustomerId.isBlank()) {
            userRepository.findById(UserId.of(userId)).ifPresent(user -> {
                if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()) {
                    user.assignStripeCustomerId(stripeCustomerId);
                    userRepository.save(user);
                }
            });
        }

        final var stripeSubscriptionId = session.getSubscription();
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            log.warn("Checkout session missing subscription id: {}", session.getId());
            return;
        }

        final var subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseGet(() -> Subscription.start(UserId.of(userId), plan, stripeSubscriptionId));

        subscriptionRepository.save(subscription);
    }

    public void handleSubscriptionChange(StripeSubscription stripeSubscription, ChangeType changeType) {
        final var stripeSubscriptionId = stripeSubscription.id();
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            log.warn("Stripe subscription missing id");
            return;
        }

        if (stripeSubscription.customer() != null && !stripeSubscription.customer().isBlank()) {
            userRepository.findByStripeCustomerId(stripeSubscription.customer()).ifPresent(user -> {
                if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()) {
                    user.assignStripeCustomerId(stripeSubscription.customer());
                    userRepository.save(user);
                }
            });
        }

        final var currentPeriodEnd = toInstant(stripeSubscription.currentPeriodEnd());
        final var status = mapStatus(stripeSubscription.status());

        final var existing = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
        final Subscription subscription;
        if (existing.isEmpty()) {
            final var userId = findUserIdForStripeSubscription(stripeSubscription);
            final var plan = extractPlanFromSubscription(stripeSubscription);
            if (userId == null || plan == null) {
                log.info("No local subscription found for Stripe id: {}", stripeSubscriptionId);
                return;
            }
            subscription = Subscription.start(UserId.of(userId), plan, stripeSubscriptionId);
        } else {
            subscription = existing.get();
        }
        if (changeType == ChangeType.DELETED) {
            subscription.updateStatus(SubscriptionStatus.CANCELED, currentPeriodEnd);
        } else {
            subscription.updateStatus(status, currentPeriodEnd);
        }

        subscriptionRepository.save(subscription);
    }

    private String extractUserId(Session session) {
        if (session.getMetadata() == null) {
            return null;
        }
        return session.getMetadata().get("userId");
    }

    private SubscriptionPlan extractPlan(Session session) {
        if (session.getMetadata() == null) {
            return null;
        }
        final var plan = session.getMetadata().get("plan");
        if (plan == null) {
            return null;
        }
        return switch (plan.toUpperCase()) {
            case "BASIC" -> SubscriptionPlan.BASIC;
            case "PRO" -> SubscriptionPlan.PRO;
            default -> null;
        };
    }

    private String findUserIdForStripeSubscription(StripeSubscription stripeSubscription) {
        final var stripeCustomerId = stripeSubscription.customer();
        if (stripeCustomerId == null || stripeCustomerId.isBlank()) {
            return null;
        }
        return userRepository.findByStripeCustomerId(stripeCustomerId)
                .map(user -> user.getId().value())
                .orElse(null);
    }

    private SubscriptionPlan extractPlanFromSubscription(StripeSubscription stripeSubscription) {
        if (stripeSubscription.metadata() == null) {
            return null;
        }
        final var plan = stripeSubscription.metadata().get("plan");
        if (plan == null) {
            return null;
        }
        return switch (plan.toUpperCase()) {
            case "BASIC" -> SubscriptionPlan.BASIC;
            case "PRO" -> SubscriptionPlan.PRO;
            default -> null;
        };
    }

    private SubscriptionStatus mapStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return SubscriptionStatus.PAST_DUE;
        }
        return switch (stripeStatus) {
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "active" -> SubscriptionStatus.ACTIVE;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled", "unpaid" -> SubscriptionStatus.CANCELED;
            default -> SubscriptionStatus.PAST_DUE;
        };
    }

    private Instant toInstant(Long epochSeconds) {
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
    }
}
