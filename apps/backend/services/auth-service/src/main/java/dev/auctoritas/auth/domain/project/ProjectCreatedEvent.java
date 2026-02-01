package dev.auctoritas.auth.domain.project;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new project is created.
 */
public record ProjectCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID organizationId,
    String name,
    String slug,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "project.created";
  }
  
  public UUID projectId() {
    return aggregateId;
  }
}
