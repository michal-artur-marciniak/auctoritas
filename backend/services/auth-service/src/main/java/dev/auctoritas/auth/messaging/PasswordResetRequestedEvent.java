package dev.auctoritas.auth.messaging;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetRequestedEvent(
    UUID projectId,
    UUID userId,
    String email,
    String resetToken,
    Instant expiresAt,
    String ipAddress,
    String userAgent) {
  public static final String EVENT_TYPE = "user.password_reset_requested";
}
