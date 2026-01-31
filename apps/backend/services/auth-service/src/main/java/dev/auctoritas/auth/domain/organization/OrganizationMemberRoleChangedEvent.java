package dev.auctoritas.auth.domain.organization;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an organization member's role changes.
 */
public record OrganizationMemberRoleChangedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID organizationId,
    OrganizationMemberRole previousRole,
    OrganizationMemberRole newRole,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "organization.member.role_changed";
  }

  public UUID memberId() {
    return aggregateId;
  }
}
