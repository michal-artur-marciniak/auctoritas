package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.application.port.in.enduser.EndUserLoginResult;

/**
 * Use case for completing an MFA challenge during login.
 * Validates the TOTP code and issues tokens.
 * Implements UC-004 from PRD.
 */
public interface CompleteMfaChallengeUseCase {
  /**
   * Completes an MFA challenge by validating the TOTP code.
   * On success, issues access and refresh tokens.
   *
   * @param apiKey the API key for the project
   * @param mfaToken the MFA challenge token
   * @param code the 6-digit TOTP code
   * @param ipAddress the client IP address
   * @param userAgent the client user agent
   * @return login response with tokens
   */
  EndUserLoginResult completeChallenge(
      String apiKey,
      String mfaToken,
      String code,
      String ipAddress,
      String userAgent);
}
