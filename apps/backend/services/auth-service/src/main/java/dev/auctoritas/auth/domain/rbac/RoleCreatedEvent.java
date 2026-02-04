package dev.auctoritas.auth.domain.rbac;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a role is created.
 */
public record RoleCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID projectId,
    String name,
    boolean system,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "role.created";
  }
}
