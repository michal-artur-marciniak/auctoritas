package dev.auctoritas.auth.domain.model.project;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new API key is created.
 */
public record ApiKeyCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID projectId,
    String name,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "apikey.created";
  }
  
  public UUID apiKeyId() {
    return aggregateId;
  }
}
