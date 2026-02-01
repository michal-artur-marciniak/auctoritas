package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.mfa.SetupMfaResult;

/**
 * Use case for setting up MFA for an end user.
 * Implements SetupMfaPort (UC-001 from PRD).
 */
public interface SetupMfaUseCase {
  /**
   * Initiates MFA setup for an end user.
   * Generates TOTP secret, recovery codes, and creates the EndUserMfa aggregate.
   * MFA is not enabled until verified.
   *
   * @param apiKey the API key for the project
   * @param principal the authenticated end user
   * @return setup result containing secret, QR code URL, and backup codes
   */
  SetupMfaResult setupMfa(String apiKey, EndUserPrincipal principal);
}
