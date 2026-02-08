package com.example.api.infrastructure.stripe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StripeWebhookHandlerTest {

    private MockMvc mockMvc;

    private StripeSubscriptionService subscriptionService;

    private StripeProperties stripeProperties;

    private StripeWebhookHandler handler;

    @BeforeEach
    void setUp() {
        subscriptionService = Mockito.mock(StripeSubscriptionService.class);
        stripeProperties = Mockito.mock(StripeProperties.class);
        handler = new StripeWebhookHandler(stripeProperties, subscriptionService);
        mockMvc = MockMvcBuilders.standaloneSetup(handler).build();
    }

    @Test
    void rejectsWhenWebhookSecretMissing() throws Exception {
        when(stripeProperties.getWebhookSecret()).thenReturn("");

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Stripe-Signature", "sig"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void acceptsWhenWebhookSecretConfigured() throws Exception {
        when(stripeProperties.getWebhookSecret()).thenReturn("whsec_test");
        when(stripeProperties.getSecretKey()).thenReturn("sk_test");

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mockEventPayload())
                        .header("Stripe-Signature", "t=0,v1=invalid"))
                .andExpect(status().isBadRequest());
    }

    private String mockEventPayload() {
        return "{\"id\":\"evt_123\",\"type\":\"customer.subscription.updated\",\"data\":{\"object\":{}}}";
    }
}
