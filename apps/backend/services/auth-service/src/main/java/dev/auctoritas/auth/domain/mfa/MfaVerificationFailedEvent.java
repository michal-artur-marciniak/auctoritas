package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an MFA verification fails.
 * Used for rate limiting and security monitoring.
 */
public record MfaVerificationFailedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    UUID projectId,
    int attemptNumber,
    int maxAttempts,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "mfa.verification.failed";
  }
}
