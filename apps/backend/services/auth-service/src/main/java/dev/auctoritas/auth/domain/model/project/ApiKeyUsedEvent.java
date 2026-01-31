package dev.auctoritas.auth.domain.model.project;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an API key is used (lastUsedAt is updated).
 */
public record ApiKeyUsedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID projectId,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "apikey.used";
  }
  
  public UUID apiKeyId() {
    return aggregateId;
  }
}
