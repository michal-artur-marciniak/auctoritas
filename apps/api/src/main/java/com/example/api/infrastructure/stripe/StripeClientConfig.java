package com.example.api.infrastructure.stripe;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the Stripe SDK with the configured secret key.
 */
@Configuration
public class StripeClientConfig {

    private final StripeProperties properties;

    public StripeClientConfig(StripeProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (!properties.getSecretKey().isBlank()) {
            Stripe.apiKey = properties.getSecretKey();
        }
    }
}
