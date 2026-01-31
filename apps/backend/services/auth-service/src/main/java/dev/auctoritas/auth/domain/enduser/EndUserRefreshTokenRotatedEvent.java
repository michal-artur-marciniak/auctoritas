package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an end-user refresh token is rotated (replaced by a new one).
 */
public record EndUserRefreshTokenRotatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    UUID newTokenId,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "enduser.refreshtoken.rotated";
  }
  
  public UUID oldTokenId() {
    return aggregateId;
  }
}
