package dev.auctoritas.auth.application.port.in.enduser;

import java.util.UUID;

/**
 * Application-layer login result with tokens and user summary.
 */
public record EndUserLoginResult(
    EndUserSummary user,
    String accessToken,
    String refreshToken,
    Boolean mfaRequired,
    String mfaToken) {

  public record EndUserSummary(UUID id, String email, String name, boolean emailVerified) {}

  public static EndUserLoginResult success(EndUserSummary user, String accessToken, String refreshToken) {
    return new EndUserLoginResult(user, accessToken, refreshToken, false, null);
  }

  public static EndUserLoginResult mfaChallenge(String mfaToken) {
    return new EndUserLoginResult(null, null, null, true, mfaToken);
  }
}
