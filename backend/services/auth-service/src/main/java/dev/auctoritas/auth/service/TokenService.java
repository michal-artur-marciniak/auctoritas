package dev.auctoritas.auth.service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private static final int ACCESS_TOKEN_BYTES = 32;
  private static final int REFRESH_TOKEN_BYTES = 48;

  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public String generateAccessToken() {
    return generateToken(ACCESS_TOKEN_BYTES);
  }

  public String generateRefreshToken() {
    return generateToken(REFRESH_TOKEN_BYTES);
  }

  private String generateToken(int length) {
    byte[] buffer = new byte[length];
    secureRandom.nextBytes(buffer);
    return encoder.encodeToString(buffer);
  }
}
