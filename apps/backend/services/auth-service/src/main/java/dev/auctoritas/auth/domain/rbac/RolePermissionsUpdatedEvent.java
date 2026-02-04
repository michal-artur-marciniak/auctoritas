package dev.auctoritas.auth.domain.rbac;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event emitted when role permissions are updated.
 */
public record RolePermissionsUpdatedEvent(
    UUID eventId,
    UUID aggregateId,
    List<String> permissionCodes,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "role.permissions.updated";
  }
}
