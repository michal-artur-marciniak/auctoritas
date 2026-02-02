package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.application.port.in.mfa.EndUserMfaPrincipal;

/**
 * Use case for verifying MFA setup for an end user.
 * Implements UC-002 from PRD.
 */
public interface VerifyMfaUseCase {
  /**
   * Verifies the TOTP code and enables MFA for the user.
   * On successful verification, recovery codes are persisted and MFA is enabled.
   *
   * @param apiKey the API key for the project
   * @param principal the authenticated end user
   * @param code the 6-digit TOTP code to verify
   */
  void verifyMfa(String apiKey, EndUserMfaPrincipal principal, String code);
}
