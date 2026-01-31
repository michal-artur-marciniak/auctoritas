package dev.auctoritas.auth.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a password reset is requested.
 *
 * <p>Security note: this event does NOT include a plaintext reset token. Email/worker consumers
 * should generate reset links via a secure mechanism (e.g. call auth-service to create a link)
 * rather than relying on a secret carried in the message.
 */
public record PasswordResetRequestedEvent(
    UUID projectId,
    UUID userId,
    String email,
    UUID resetTokenId,
    String resetTokenHash,
    Instant expiresAt,
    String ipAddress,
    String userAgent) {
  public static final String EVENT_TYPE = "user.password_reset_requested";
}
