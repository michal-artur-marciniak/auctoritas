package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an organization is renamed.
 */
public record OrganizationRenamedEvent(
    UUID eventId,
    UUID aggregateId,
    String oldName,
    String newName,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "organization.renamed";
  }

  public UUID organizationId() {
    return aggregateId;
  }
}
