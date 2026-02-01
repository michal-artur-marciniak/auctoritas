package dev.auctoritas.auth.domain.organization;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an organization member refresh token is rotated (replaced by a new one).
 */
public record OrganizationMemberRefreshTokenRotatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID memberId,
    UUID newTokenId,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "orgmember.refreshtoken.rotated";
  }
  
  public UUID oldTokenId() {
    return aggregateId;
  }
}
