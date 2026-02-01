package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.mfa.RegenerateRecoveryCodesResult;

/**
 * Use case for regenerating recovery codes for an end user.
 * Implements UC-007 from PRD.
 */
public interface RegenerateRecoveryCodesUseCase {
  /**
   * Regenerates recovery codes for the authenticated end user.
   * Requires TOTP code verification before regenerating.
   * Old recovery codes are invalidated and replaced with new ones.
   *
   * @param apiKey the API key for the project
   * @param principal the authenticated end user
   * @param code the TOTP code to verify before regenerating
   * @return result containing the new recovery codes (shown once)
   */
  RegenerateRecoveryCodesResult regenerateRecoveryCodes(String apiKey, EndUserPrincipal principal, String code);
}
