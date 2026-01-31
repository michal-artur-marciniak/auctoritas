package dev.auctoritas.auth.domain.model.project;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an API key is revoked.
 */
public record ApiKeyRevokedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID projectId,
    String reason,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "apikey.revoked";
  }
  
  public UUID apiKeyId() {
    return aggregateId;
  }
}
