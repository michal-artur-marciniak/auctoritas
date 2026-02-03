package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.adapter.in.web.OrgLoginResponse;

/**
 * Use case for completing an MFA challenge during organization member login.
 * Validates the TOTP code and issues tokens.
 * Implements UC-004 for organization members from the PRD.
 */
public interface CompleteOrgMemberMfaChallengeUseCase {
  /**
   * Completes an MFA challenge by validating the TOTP code.
   * On success, issues access and refresh tokens.
   *
   * @param mfaToken the MFA challenge token
   * @param code the 6-digit TOTP code
   * @param ipAddress the client IP address
   * @param userAgent the client user agent
   * @return login response with tokens
   */
  OrgLoginResponse completeChallenge(
      String mfaToken,
      String code,
      String ipAddress,
      String userAgent);
}
