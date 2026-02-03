package dev.auctoritas.auth.application.port.in.mfa;

import java.util.Objects;
import java.util.UUID;

/**
 * Application-layer representation of an authenticated end user.
 * Decouples MFA use cases from infrastructure security principals.
 *
 * @param userId the end-user identifier
 * @param projectId the project identifier
 * @param email the end-user email address
 */
public record EndUserMfaPrincipal(
    UUID userId,
    UUID projectId,
    String email) {

  public EndUserMfaPrincipal {
    Objects.requireNonNull(userId, "userId required");
    Objects.requireNonNull(projectId, "projectId required");
    Objects.requireNonNull(email, "email required");
  }
}
