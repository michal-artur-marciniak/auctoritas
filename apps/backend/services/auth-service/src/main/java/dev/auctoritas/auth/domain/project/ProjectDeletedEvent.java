package dev.auctoritas.auth.domain.project;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a project is marked as deleted.
 */
public record ProjectDeletedEvent(
    UUID eventId,
    UUID aggregateId,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "project.deleted";
  }
  
  public UUID projectId() {
    return aggregateId;
  }
}
