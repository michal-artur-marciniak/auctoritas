package dev.auctoritas.auth.domain.oauth;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new OAuth connection is established.
 */
public record OAuthConnectionEstablishedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID userId,
    UUID projectId,
    String provider,
    String providerUserId,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "oauth.connection.established";
  }
  
  public UUID connectionId() {
    return aggregateId;
  }
}
