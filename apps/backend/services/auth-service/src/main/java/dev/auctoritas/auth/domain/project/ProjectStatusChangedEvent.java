package dev.auctoritas.auth.domain.project;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import dev.auctoritas.auth.domain.project.ProjectStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a project's status changes.
 */
public record ProjectStatusChangedEvent(
    UUID eventId,
    UUID aggregateId,
    ProjectStatus previousStatus,
    ProjectStatus newStatus,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "project.status_changed";
  }
  
  public UUID projectId() {
    return aggregateId;
  }
}
