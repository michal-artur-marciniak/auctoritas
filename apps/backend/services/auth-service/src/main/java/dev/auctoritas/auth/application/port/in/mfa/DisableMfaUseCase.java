package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.application.port.in.mfa.EndUserMfaPrincipal;

/**
 * Use case for disabling MFA for an end user.
 * Implements UC-006 from PRD.
 */
public interface DisableMfaUseCase {
  /**
   * Disables MFA for the authenticated end user.
   * Requires TOTP code verification before disabling.
   * All recovery codes are deleted when MFA is disabled.
   *
   * @param apiKey the API key for the project
   * @param principal the authenticated end user
   * @param code the TOTP code to verify before disabling
   */
  void disableMfa(String apiKey, EndUserMfaPrincipal principal, String code);
}
