package dev.auctoritas.auth.domain.model.enduser;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new end-user account is registered.
 */
public record UserRegisteredEvent(
    UUID eventId,
    UUID aggregateId,
    String email,
    String name,
    boolean emailVerified,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "user.registered";
  }
  
  public UUID projectId() {
    return aggregateId;  // In this case, aggregateId is the user ID
  }
  
  public UUID userId() {
    return aggregateId;
  }
}
