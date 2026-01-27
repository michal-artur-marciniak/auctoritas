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
  private static final int EMAIL_VERIFICATION_TOKEN_BYTES = 32;
  private static final Duration EMAIL_VERIFICATION_TOKEN_TTL = Duration.ofHours(24);

  private static final int OAUTH_EXCHANGE_CODE_BYTES = 32;
  private static final Duration OAUTH_EXCHANGE_CODE_TTL = Duration.ofMinutes(2);

  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public String generateRefreshToken() {
    return generateToken(REFRESH_TOKEN_BYTES);
  }

  public String generatePasswordResetToken() {
    return generateToken(PASSWORD_RESET_TOKEN_BYTES);
  }

  public String generateEmailVerificationToken() {
    return generateToken(EMAIL_VERIFICATION_TOKEN_BYTES);
  }

  public String generateOAuthExchangeCode() {
    return generateToken(OAUTH_EXCHANGE_CODE_BYTES);
  }

  public String generateEmailVerificationCode() {
    return String.format("%06d", secureRandom.nextInt(1_000_000));
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

  public Instant getEmailVerificationTokenExpiry() {
    return Instant.now().plus(EMAIL_VERIFICATION_TOKEN_TTL);
  }

  public Instant getOAuthExchangeCodeExpiry() {
    return Instant.now().plus(OAUTH_EXCHANGE_CODE_TTL);
  }

  private String generateToken(int length) {
    byte[] buffer = new byte[length];
    secureRandom.nextBytes(buffer);
    return encoder.encodeToString(buffer);
  }
}
