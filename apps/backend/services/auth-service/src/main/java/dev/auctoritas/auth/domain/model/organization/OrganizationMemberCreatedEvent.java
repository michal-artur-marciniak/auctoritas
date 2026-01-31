package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.domain.event.DomainEvent;
import dev.auctoritas.auth.domain.model.organization.OrganizationMemberRole;
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
