package dev.auctoritas.auth.domain.organization;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an organization member's status changes.
 */
public record OrganizationMemberStatusChangedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID organizationId,
    OrganizationMemberStatus previousStatus,
    OrganizationMemberStatus newStatus,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "organization.member.status_changed";
  }

  public UUID memberId() {
    return aggregateId;
  }
}
