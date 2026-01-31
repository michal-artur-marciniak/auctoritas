package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted when an organization member verifies their email.
 */
public record OrganizationMemberEmailVerifiedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID organizationId,
    String email,
    Instant occurredAt
) implements DomainEvent {

  @Override
  public String eventType() {
    return "organization.member.email_verified";
  }

  public UUID memberId() {
    return aggregateId;
  }
}
