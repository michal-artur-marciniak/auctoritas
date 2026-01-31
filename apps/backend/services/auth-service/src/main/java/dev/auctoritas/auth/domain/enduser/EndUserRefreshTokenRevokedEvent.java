package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an end-user refresh token is revoked.
 */
public record EndUserRefreshTokenRevokedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    String reason,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "enduser.refreshtoken.revoked";
  }
  
  public UUID tokenId() {
    return aggregateId;
  }
}
