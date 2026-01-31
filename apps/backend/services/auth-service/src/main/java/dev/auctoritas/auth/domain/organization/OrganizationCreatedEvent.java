package dev.auctoritas.auth.domain.organization;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new organization is created.
 */
public record OrganizationCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    String name,
    String slug,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "organization.created";
  }
  
  public UUID organizationId() {
    return aggregateId;
  }
}
