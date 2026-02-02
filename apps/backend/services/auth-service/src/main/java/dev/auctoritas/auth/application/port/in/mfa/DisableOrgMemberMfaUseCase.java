package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;

/**
 * Use case for disabling MFA for an organization member.
 * Implements disable use case for org members (analogous to end user disable).
 */
public interface DisableOrgMemberMfaUseCase {
  /**
   * Disables MFA for the authenticated organization member.
   * Requires TOTP code verification before disabling.
   * All recovery codes are deleted when MFA is disabled.
   *
   * @param principal the authenticated organization member
   * @param code the TOTP code to verify before disabling
   */
  void disableMfa(OrganizationMemberPrincipal principal, String code);
}
