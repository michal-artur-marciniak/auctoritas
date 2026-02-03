package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.application.mfa.VerifyMfaResult;
import dev.auctoritas.auth.application.port.in.ApplicationPrincipal;

/**
 * Use case for verifying MFA setup for an organization member.
 * Implements verify use case for org members (analogous to end user verify).
 */
public interface VerifyOrgMemberMfaUseCase {
  /**
   * Verifies the TOTP code and enables MFA for the organization member.
   * On successful verification, recovery codes are persisted and MFA is enabled.
   *
   * @param principal the authenticated organization member
   * @param code the 6-digit TOTP code to verify
   * @return result containing recovery codes (shown once)
   */
  VerifyMfaResult verifyMfa(ApplicationPrincipal principal, String code);
}
