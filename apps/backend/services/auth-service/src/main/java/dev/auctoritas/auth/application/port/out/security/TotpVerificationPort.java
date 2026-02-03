package dev.auctoritas.auth.application.port.out.security;

/**
 * Port for TOTP (Time-based One-Time Password) verification.
 */
public interface TotpVerificationPort {

  /**
   * Verifies a TOTP code against a secret.
   * Validates the code with a small time window tolerance to account for clock skew.
   *
   * @param secret the Base32-encoded TOTP secret
   * @param code the 6-digit TOTP code to verify
   * @return true if the code is valid
   */
  boolean verify(String secret, String code);

  /**
   * Verifies a TOTP code against a secret with a specific time window.
   *
   * @param secret the Base32-encoded TOTP secret
   * @param code the 6-digit TOTP code to verify
   * @param timeWindow the number of time steps to check before/after current (e.g., 1 = ±30 seconds)
   * @return true if the code is valid
   */
  boolean verify(String secret, String code, int timeWindow);

  /**
   * Generates the current TOTP code for a secret.
   * Useful for testing purposes.
   *
   * @param secret the Base32-encoded TOTP secret
   * @return the current 6-digit TOTP code
   */
  String generateCurrentCode(String secret);
}
