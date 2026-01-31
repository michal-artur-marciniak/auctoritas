package dev.auctoritas.auth.domain.project;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a project is renamed.
 */
public record ProjectRenamedEvent(
    UUID eventId,
    UUID aggregateId,
    String oldName,
    String newName,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "project.renamed";
  }
  
  public UUID projectId() {
    return aggregateId;
  }
}
