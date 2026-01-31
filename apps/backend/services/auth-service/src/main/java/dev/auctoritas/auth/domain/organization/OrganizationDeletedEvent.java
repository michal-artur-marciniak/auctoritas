package dev.auctoritas.auth.domain.organization;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an organization is marked as deleted.
 */
public record OrganizationDeletedEvent(
    UUID eventId,
    UUID aggregateId,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "organization.deleted";
  }

  public UUID organizationId() {
    return aggregateId;
  }
}
