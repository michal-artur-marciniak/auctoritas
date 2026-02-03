package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.application.port.in.enduser.EndUserLoginResult;

/**
 * Use case for using a recovery code to complete login.
 * Validates the recovery code, marks it as used, and issues tokens.
 * Implements UC-004 from PRD.
 */
public interface UseRecoveryCodeUseCase {
  /**
   * Uses a recovery code to complete an MFA challenge.
   * The recovery code is marked as used and cannot be used again.
   *
   * @param apiKey the API key for the project
   * @param mfaToken the MFA challenge token
   * @param recoveryCode the recovery code
   * @param ipAddress the client IP address
   * @param userAgent the client user agent
   * @return login response with tokens
   */
  EndUserLoginResult useRecoveryCode(
      String apiKey,
      String mfaToken,
      String recoveryCode,
      String ipAddress,
      String userAgent);
}
