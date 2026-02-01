package dev.auctoritas.auth.domain.oauth;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an OAuth connection's email is updated.
 */
public record OAuthConnectionEmailUpdatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    String oldEmail,
    String newEmail,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "oauth.connection.email_updated";
  }
  
  public UUID connectionId() {
    return aggregateId;
  }
}
