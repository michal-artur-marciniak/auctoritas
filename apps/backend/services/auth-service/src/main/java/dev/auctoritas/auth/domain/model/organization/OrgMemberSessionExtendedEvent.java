package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an organization member session is extended/refreshed.
 */
public record OrgMemberSessionExtendedEvent(
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
