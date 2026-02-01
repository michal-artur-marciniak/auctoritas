package dev.auctoritas.auth.ports.security;

import java.time.Instant;

/**
 * Port for generating and hashing security tokens used by auth flows.
 */
public interface TokenHasherPort {
  String generateRefreshToken();

  String generatePasswordResetToken();

  String generateEmailVerificationToken();

  String generateOAuthExchangeCode();

  String generateOAuthState();

  String generateOAuthCodeVerifier();

  String generateEmailVerificationCode();

  String hashToken(String token);

  Instant getRefreshTokenExpiry();

  Instant getPasswordResetTokenExpiry();

  Instant getEmailVerificationTokenExpiry();

  Instant getOAuthExchangeCodeExpiry();
}
