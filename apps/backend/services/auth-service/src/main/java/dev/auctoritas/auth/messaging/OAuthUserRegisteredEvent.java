package dev.auctoritas.auth.messaging;

import java.util.UUID;

/** Domain event emitted when an end-user account is registered via OAuth. */
public record OAuthUserRegisteredEvent(
    UUID projectId,
    UUID userId,
    String email,
    String name,
    boolean emailVerified,
    String provider,
    String providerUserId) {
  public static final String EVENT_TYPE = "user.registered.oauth";
}
