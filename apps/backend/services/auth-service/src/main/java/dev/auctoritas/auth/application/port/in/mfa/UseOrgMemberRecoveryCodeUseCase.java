package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.adapter.in.web.OrgLoginResponse;

/**
 * Use case for using a recovery code to complete organization member login.
 * Validates the recovery code, marks it as used, and issues tokens.
 * Implements UC-005 for organization members from the PRD.
 */
public interface UseOrgMemberRecoveryCodeUseCase {
  /**
   * Uses a recovery code to complete an MFA challenge.
   * The recovery code is marked as used and cannot be used again.
   *
   * @param mfaToken the MFA challenge token
   * @param recoveryCode the recovery code
   * @param ipAddress the client IP address
   * @param userAgent the client user agent
   * @return login response with tokens
   */
  OrgLoginResponse useRecoveryCode(
      String mfaToken,
      String recoveryCode,
      String ipAddress,
      String userAgent);
}
