package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an end-user session is invalidated/logged out.
 */
public record EndUserSessionInvalidatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    String reason,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "enduser.session.invalidated";
  }
  
  public UUID sessionId() {
    return aggregateId;
  }
}
