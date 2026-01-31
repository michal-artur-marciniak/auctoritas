package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new organization member refresh token is created.
 */
public record OrganizationMemberRefreshTokenCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID memberId,
    Instant expiresAt,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "orgmember.refreshtoken.created";
  }
  
  public UUID tokenId() {
    return aggregateId;
  }
}
