package dev.auctoritas.auth.messaging;

import java.time.Instant;
import java.util.UUID;

/** Domain event emitted when an end-user requests a new email verification challenge. */
public record EmailVerificationResentEvent(
    UUID projectId,
    UUID userId,
    String email,
    UUID emailVerificationTokenId,
    Instant emailVerificationExpiresAt) {
  public static final String EVENT_TYPE = "user.email_verification_resent";
}
