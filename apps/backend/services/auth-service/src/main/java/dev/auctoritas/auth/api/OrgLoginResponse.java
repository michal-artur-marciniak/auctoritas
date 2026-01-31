package dev.auctoritas.auth.api;

import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import java.util.UUID;

public record OrgLoginResponse(
    OrganizationSummary organization,
    MemberSummary member,
    String accessToken,
    String refreshToken) {

  public record OrganizationSummary(UUID id, String name, String slug) {}

  public record MemberSummary(UUID id, String email, OrganizationMemberRole role) {}
}
