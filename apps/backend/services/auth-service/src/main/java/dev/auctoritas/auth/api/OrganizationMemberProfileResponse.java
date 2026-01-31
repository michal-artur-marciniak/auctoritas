package dev.auctoritas.auth.api;

import dev.auctoritas.auth.domain.model.organization.OrganizationMemberRole;
import dev.auctoritas.auth.domain.model.organization.OrganizationMemberStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for the /api/v1/org/me endpoint.
 * Contains the authenticated org member's profile and organization info.
 */
public record OrganizationMemberProfileResponse(
    UUID id,
    String email,
    String name,
    String avatarUrl,
    OrganizationMemberRole role,
    OrganizationMemberStatus status,
    Boolean emailVerified,
    Boolean mfaEnabled,
    Instant createdAt,
    OrganizationInfo organization) {

  public record OrganizationInfo(UUID id, String name, String slug) {}
}
