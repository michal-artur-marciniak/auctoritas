package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import java.util.UUID;

/**
 * Response for organization member login.
 * Can either contain the full login tokens or an MFA challenge.
 */
public record OrgLoginResponse(
    OrganizationSummary organization,
    MemberSummary member,
    String accessToken,
    String refreshToken,
    Boolean mfaRequired,
    String mfaToken) {

  public record OrganizationSummary(UUID id, String name, String slug) {}

  public record MemberSummary(UUID id, String email, OrganizationMemberRole role) {}

  /**
   * Creates a successful login response with tokens.
   */
  public static OrgLoginResponse success(
      OrganizationSummary organization,
      MemberSummary member,
      String accessToken,
      String refreshToken) {
    return new OrgLoginResponse(organization, member, accessToken, refreshToken, false, null);
  }

  /**
   * Creates an MFA challenge response requiring TOTP verification.
   */
  public static OrgLoginResponse mfaChallenge(String mfaToken) {
    return new OrgLoginResponse(null, null, null, null, true, mfaToken);
  }
}
