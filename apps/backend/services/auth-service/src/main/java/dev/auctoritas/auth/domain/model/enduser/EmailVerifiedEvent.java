package dev.auctoritas.auth.domain.model.enduser;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a user's email address is verified.
 */
public record EmailVerifiedEvent(
    UUID eventId,
    UUID aggregateId,
    String email,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "user.email_verified";
  }
  
  public UUID userId() {
    return aggregateId;
  }
}
