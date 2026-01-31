package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new end-user refresh token is created.
 */
public record EndUserRefreshTokenCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    Instant expiresAt,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "enduser.refreshtoken.created";
  }
  
  public UUID tokenId() {
    return aggregateId;
  }
}
