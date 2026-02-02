package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;
import dev.auctoritas.auth.application.mfa.RegenerateRecoveryCodesResult;

/**
 * Use case for regenerating recovery codes for an organization member.
 * Implements regenerate use case for org members (analogous to end user regenerate).
 */
public interface RegenerateOrgMemberRecoveryCodesUseCase {
  /**
   * Regenerates recovery codes for the authenticated organization member.
   * Requires TOTP code verification before regenerating.
   * Old recovery codes are invalidated and replaced with new ones.
   *
   * @param principal the authenticated organization member
   * @param code the TOTP code to verify before regenerating
   * @return result containing the new recovery codes (shown once)
   */
  RegenerateRecoveryCodesResult regenerateRecoveryCodes(OrganizationMemberPrincipal principal, String code);
}
