package dev.auctoritas.auth.domain.model.enduser;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new end-user session is created.
 */
public record EndUserSessionCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    String ipAddress,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "enduser.session.created";
  }
  
  public UUID sessionId() {
    return aggregateId;
  }
}
