package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an end-user session is extended/refreshed.
 */
public record EndUserSessionExtendedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    Instant newExpiresAt,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "enduser.session.extended";
  }
  
  public UUID sessionId() {
    return aggregateId;
  }
}
