package com.example.api.infrastructure.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Stripe integration.
 */
@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private String secretKey = "";
    private String webhookSecret = "";

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey == null ? "" : secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret;
    }
}
