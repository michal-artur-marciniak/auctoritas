package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an organization member refresh token is revoked.
 */
public record OrganizationMemberRefreshTokenRevokedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID memberId,
    String reason,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "orgmember.refreshtoken.revoked";
  }
  
  public UUID tokenId() {
    return aggregateId;
  }
}
