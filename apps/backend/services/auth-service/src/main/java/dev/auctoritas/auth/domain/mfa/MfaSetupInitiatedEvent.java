package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a user begins MFA setup.
 */
public record MfaSetupInitiatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    UUID projectId,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "mfa.setup.initiated";
  }
}
