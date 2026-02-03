package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.application.mfa.SetupMfaResult;

/**
 * MFA setup port for end-user setup flows.
 * See US-002/US-003 from PRD.
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
  SetupMfaResult setupMfa(String apiKey, EndUserMfaPrincipal principal);
}
