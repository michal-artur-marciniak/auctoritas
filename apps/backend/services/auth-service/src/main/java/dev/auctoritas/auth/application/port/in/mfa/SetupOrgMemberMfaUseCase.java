package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;
import dev.auctoritas.auth.application.mfa.SetupMfaResult;

/**
 * Use case for setting up MFA for an organization member.
 * Implements setup use case for org members (analogous to end user setup).
 */
public interface SetupOrgMemberMfaUseCase {
  /**
   * Initiates MFA setup for an organization member.
   * Generates TOTP secret, recovery codes, and creates the OrganizationMemberMfa aggregate.
   * MFA is not enabled until verified.
   *
   * @param principal the authenticated organization member
   * @return setup result containing secret, QR code URL, and backup codes
   */
  SetupMfaResult setupMfa(OrganizationMemberPrincipal principal);
}
