package dev.auctoritas.auth.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

/** Domain event emitted when an end-user account is registered. */
public record UserRegisteredEvent(
    UUID projectId,
    UUID userId,
    String email,
    String name,
    boolean emailVerified,
    UUID emailVerificationTokenId,
    Instant emailVerificationExpiresAt) {
  public static final String EVENT_TYPE = "user.registered";
}
