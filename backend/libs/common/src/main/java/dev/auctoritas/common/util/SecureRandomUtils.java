package dev.auctoritas.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public class SecureRandomUtils {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int DEFAULT_TOKEN_LENGTH = 32;

  public static String generateSecureString() {
    return generateSecureString(DEFAULT_TOKEN_LENGTH);
  }

  public static String generateSecureString(int length) {
    byte[] bytes = new byte[length];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static byte[] generateSecureBytes(int length) {
    byte[] bytes = new byte[length];
    SECURE_RANDOM.nextBytes(bytes);
    return bytes;
  }

  public static int generateSecureInt(int bound) {
    return SECURE_RANDOM.nextInt(bound);
  }
}
