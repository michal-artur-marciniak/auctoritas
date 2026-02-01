package dev.auctoritas.auth.domain.organization;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an organization's status changes.
 */
public record OrganizationStatusChangedEvent(
    UUID eventId,
    UUID aggregateId,
    OrganizationStatus previousStatus,
    OrganizationStatus newStatus,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "organization.status_changed";
  }

  public UUID organizationId() {
    return aggregateId;
  }
}
