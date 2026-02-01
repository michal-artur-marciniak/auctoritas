package dev.auctoritas.auth.application.port.in;

import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import java.util.Objects;
import java.util.UUID;

/**
 * Application-layer representation of an authenticated organization member.
 *
 * <p>This DTO decouples use-cases from infrastructure concerns (Spring Security).
 * Controllers map adapter-specific principal types (e.g., OrganizationMemberPrincipal)
 * to this application-layer DTO before invoking use-cases.
 *
 * @param memberId the organization member's unique identifier
 * @param orgId the organization identifier
 * @param email the member's email address
 * @param role the member's role in the organization
 */
public record ApplicationPrincipal(
    UUID memberId,
    UUID orgId,
    String email,
    OrganizationMemberRole role) {

  public ApplicationPrincipal {
    Objects.requireNonNull(memberId, "memberId required");
    Objects.requireNonNull(orgId, "orgId required");
    Objects.requireNonNull(email, "email required");
    Objects.requireNonNull(role, "role required");
  }
}
