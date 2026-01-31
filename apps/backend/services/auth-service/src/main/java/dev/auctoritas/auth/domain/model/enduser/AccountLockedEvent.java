package dev.auctoritas.auth.domain.model.enduser;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a user account is locked due to failed login attempts.
 */
public record AccountLockedEvent(
    UUID eventId,
    UUID aggregateId,
    String email,
    Instant lockedUntil,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "user.account_locked";
  }
  
  public UUID userId() {
    return aggregateId;
  }
}
