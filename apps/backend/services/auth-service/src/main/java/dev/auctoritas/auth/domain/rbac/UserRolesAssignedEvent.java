package dev.auctoritas.auth.domain.rbac;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event emitted when user role assignments are replaced.
 */
public record UserRolesAssignedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID projectId,
    List<UUID> roleIds,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "user.roles.assigned";
  }

  public UUID userId() {
    return aggregateId;
  }
}
