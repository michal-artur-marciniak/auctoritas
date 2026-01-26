package dev.auctoritas.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private static final int REFRESH_TOKEN_BYTES = 48;
  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);
  private static final int PASSWORD_RESET_TOKEN_BYTES = 32;
  private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofHours(1);

  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public String generateRefreshToken() {
    return generateToken(REFRESH_TOKEN_BYTES);
  }

  public String generatePasswordResetToken() {
    return generateToken(PASSWORD_RESET_TOKEN_BYTES);
  }

  public String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return encoder.encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  public Instant getRefreshTokenExpiry() {
    return Instant.now().plus(REFRESH_TOKEN_TTL);
  }

  public Instant getPasswordResetTokenExpiry() {
    return Instant.now().plus(PASSWORD_RESET_TOKEN_TTL);
  }

  private String generateToken(int length) {
    byte[] buffer = new byte[length];
    secureRandom.nextBytes(buffer);
    return encoder.encodeToString(buffer);
  }
}
