package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new organization member session is created.
 */
public record OrganizationMemberSessionCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID memberId,
    String ipAddress,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "orgmember.session.created";
  }
  
  public UUID sessionId() {
    return aggregateId;
  }
}
