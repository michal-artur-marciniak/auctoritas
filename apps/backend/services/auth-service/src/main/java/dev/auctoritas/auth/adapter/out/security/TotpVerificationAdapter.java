package dev.auctoritas.auth.adapter.out.security;

import dev.auctoritas.auth.application.port.out.security.TotpVerificationPort;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

/**
 * Adapter for TOTP verification using standard Java crypto.
 * Implements RFC 6238 (TOTP) using HMAC-SHA1.
 */
@Component
public class TotpVerificationAdapter implements TotpVerificationPort {

  private static final String ALGORITHM = "HmacSHA1";
  private static final int CODE_DIGITS = 6;
  private static final int TIME_STEP = 30; // seconds
  private static final int DEFAULT_TIME_WINDOW = 1; // ±1 time step

  @Override
  public boolean verify(String secret, String code) {
    return verify(secret, code, DEFAULT_TIME_WINDOW);
  }

  @Override
  public boolean verify(String secret, String code, int timeWindow) {
    if (secret == null || code == null) {
      return false;
    }

    try {
      long currentTimeStep = System.currentTimeMillis() / 1000 / TIME_STEP;

      // Check codes in the time window (before and after current time)
      for (int i = -timeWindow; i <= timeWindow; i++) {
        String expectedCode = generateCode(secret, currentTimeStep + i);
        if (expectedCode.equals(code)) {
          return true;
        }
      }

      return false;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String generateCurrentCode(String secret) {
    long currentTimeStep = System.currentTimeMillis() / 1000 / TIME_STEP;
    return generateCode(secret, currentTimeStep);
  }

  private String generateCode(String secret, long timeStep) {
    try {
      // Decode Base32 secret
      Base32 base32 = new Base32();
      byte[] key = base32.decode(secret);

      // Create HMAC-SHA1 instance
      Mac mac = Mac.getInstance(ALGORITHM);
      SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
      mac.init(keySpec);

      // Generate time byte array (big-endian, 8 bytes)
      byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

      // Calculate HMAC
      byte[] hash = mac.doFinal(timeBytes);

      // Dynamic truncation (RFC 4226)
      int offset = hash[hash.length - 1] & 0x0F;
      int binary = ((hash[offset] & 0x7F) << 24) |
                   ((hash[offset + 1] & 0xFF) << 16) |
                   ((hash[offset + 2] & 0xFF) << 8) |
                   (hash[offset + 3] & 0xFF);

      // Modulo to get 6-digit code
      int otp = binary % (int) Math.pow(10, CODE_DIGITS);

      // Pad with leading zeros
      return String.format("%0" + CODE_DIGITS + "d", otp);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException("Failed to generate TOTP code", e);
    }
  }
}
