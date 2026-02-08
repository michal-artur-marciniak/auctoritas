package com.example.api.infrastructure.stripe;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles Stripe webhook events.
 */
@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookHandler.class);

    private final StripeProperties properties;
    private final StripeSubscriptionService subscriptionService;

    public StripeWebhookHandler(StripeProperties properties, StripeSubscriptionService subscriptionService) {
        this.properties = properties;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        if (properties.getWebhookSecret().isBlank()) {
            log.warn("Stripe webhook secret not configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Webhook secret not configured");
        }

        try {
            final var event = Webhook.constructEvent(payload, signature, properties.getWebhookSecret());
            handleEvent(event);
            return ResponseEntity.ok("ok");
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Stripe webhook handling failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook handling failed");
        }
    }

    private void handleEvent(Event event) {
        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutSession(event);
            case "customer.subscription.created" -> handleSubscription(event, StripeSubscriptionService.ChangeType.CREATED);
            case "customer.subscription.updated" -> handleSubscription(event, StripeSubscriptionService.ChangeType.UPDATED);
            case "customer.subscription.deleted" -> handleSubscription(event, StripeSubscriptionService.ChangeType.DELETED);
            default -> log.debug("Unhandled Stripe event: {}", event.getType());
        }
    }

    private void handleCheckoutSession(Event event) {
        final var session = deserialize(event.getDataObjectDeserializer(), Session.class);
        if (session == null) {
            return;
        }
        subscriptionService.handleCheckoutCompleted(session);
    }

    private void handleSubscription(Event event, StripeSubscriptionService.ChangeType changeType) {
        final var subscription = StripeSubscription.from(
                deserialize(event.getDataObjectDeserializer(), Subscription.class)
        );
        if (subscription == null) {
            return;
        }
        subscriptionService.handleSubscriptionChange(subscription, changeType);
    }

    private <T> T deserialize(EventDataObjectDeserializer deserializer, Class<T> type) {
        final Object stripeObject;
        try {
            stripeObject = deserializer.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            log.warn("Stripe event payload could not be deserialized", e);
            return null;
        }
        if (stripeObject == null) {
            log.warn("Stripe event payload missing object");
            return null;
        }
        if (!type.isInstance(stripeObject)) {
            log.warn("Stripe event payload is not {}", type.getSimpleName());
            return null;
        }
        return type.cast(stripeObject);
    }
}
