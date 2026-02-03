package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when MFA is verified and enabled for a user.
 */
public record MfaEnabledEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    UUID projectId,
    String method,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "mfa.enabled";
  }
}
