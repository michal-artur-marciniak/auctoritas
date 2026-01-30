package dev.auctoritas.auth.application.enduser;

import java.util.UUID;

/**
 * Result payload for an end-user registration.
 *
 * @param user summary of the registered end user
 * @param accessToken issued access token
 * @param refreshToken issued refresh token
 */
public record EndUserRegistrationResult(
    EndUserSummary user,
    String accessToken,
    String refreshToken) {

  /**
   * Summary details for a registered end user.
   *
   * @param id end-user identifier
   * @param email end-user email address
   * @param name optional display name
   * @param emailVerified whether the email is verified
   */
  public record EndUserSummary(UUID id, String email, String name, boolean emailVerified) {}
}
