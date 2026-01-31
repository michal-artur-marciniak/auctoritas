package dev.auctoritas.auth.domain.organization;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an organization member session is extended/refreshed.
 */
public record OrganizationMemberSessionExtendedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID memberId,
    Instant newExpiresAt,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "orgmember.session.extended";
  }
  
  public UUID sessionId() {
    return aggregateId;
  }
}
