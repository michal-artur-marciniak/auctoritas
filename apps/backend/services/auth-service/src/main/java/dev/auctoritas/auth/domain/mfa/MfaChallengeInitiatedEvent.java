package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an MFA challenge is initiated during login.
 */
public record MfaChallengeInitiatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    UUID projectId,
    UUID challengeId,
    Instant expiresAt,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "mfa.challenge.initiated";
  }
}
