package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when MFA is disabled for a user.
 */
public record MfaDisabledEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    UUID projectId,
    String reason,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "mfa.disabled";
  }
}
