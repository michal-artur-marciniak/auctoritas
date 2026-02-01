package dev.auctoritas.auth.adapter.in.web;

import java.util.UUID;

/**
 * Response for end-user login.
 * Can either contain the full login tokens or an MFA challenge.
 */
public record EndUserLoginResponse(
    EndUserSummary user,
    String accessToken,
    String refreshToken,
    Boolean mfaRequired,
    String mfaToken) {

  public record EndUserSummary(UUID id, String email, String name, boolean emailVerified) {}

  /**
   * Creates a successful login response with tokens.
   */
  public static EndUserLoginResponse success(EndUserSummary user, String accessToken, String refreshToken) {
    return new EndUserLoginResponse(user, accessToken, refreshToken, false, null);
  }

  /**
   * Creates an MFA challenge response requiring TOTP verification.
   */
  public static EndUserLoginResponse mfaChallenge(String mfaToken) {
    return new EndUserLoginResponse(null, null, null, true, mfaToken);
  }
}
