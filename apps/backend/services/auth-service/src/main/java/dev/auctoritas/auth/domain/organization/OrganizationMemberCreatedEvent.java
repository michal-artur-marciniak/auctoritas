package dev.auctoritas.auth.domain.organization;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when a new organization member is created.
 */
public record OrganizationMemberCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID organizationId,
    String email,
    String name,
    OrganizationMemberRole role,
    boolean emailVerified,
    Instant occurredAt
) implements DomainEvent {
  
  @Override
  public String eventType() {
    return "organization.member.created";
  }
  
  public UUID memberId() {
    return aggregateId;
  }
}
