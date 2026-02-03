package dev.auctoritas.auth.adapter.out.security;

import dev.auctoritas.auth.application.port.out.security.EncryptionPort;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing AES-256-GCM encryption for MFA secrets.
 */
@Component
public class EncryptionAdapter implements EncryptionPort {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int TOTP_SECRET_LENGTH = 32; // 160 bits in Base32 = 32 chars
  private static final int RECOVERY_CODE_LENGTH = 10; // chars per code
  private static final int RECOVERY_CODE_GROUPS = 4; // groups per code

  private final SecretKey secretKey;
  private final int ivLength;
  private final int tagLength;
  private final SecureRandom secureRandom;

  public EncryptionAdapter(SecretKey secretKey, int ivLength, int tagLength) {
    this.secretKey = secretKey;
    this.ivLength = ivLength;
    this.tagLength = tagLength;
    this.secureRandom = new SecureRandom();
  }

  @Override
  public String encrypt(String plaintext) {
    try {
      // Generate random IV
      byte[] iv = new byte[ivLength];
      secureRandom.nextBytes(iv);

      // Initialize cipher
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(tagLength, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

      // Encrypt
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Combine IV + ciphertext
      ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
      byteBuffer.put(iv);
      byteBuffer.put(ciphertext);

      return Base64.getEncoder().encodeToString(byteBuffer.array());
    } catch (Exception e) {
      throw new SecurityException("Failed to encrypt data", e);
    }
  }

  @Override
  public String decrypt(String ciphertext) {
    try {
      byte[] decoded = Base64.getDecoder().decode(ciphertext);

      // Extract IV
      ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
      byte[] iv = new byte[ivLength];
      byteBuffer.get(iv);

      // Extract ciphertext
      byte[] encrypted = new byte[byteBuffer.remaining()];
      byteBuffer.get(encrypted);

      // Decrypt
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(tagLength, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
      byte[] plaintext = cipher.doFinal(encrypted);

      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new SecurityException("Failed to decrypt data - possible tampering or wrong key", e);
    }
  }

  @Override
  public String generateTotpSecret() {
    // Generate 160 bits (20 bytes) of random data
    byte[] randomBytes = new byte[20];
    secureRandom.nextBytes(randomBytes);

    // Encode as Base32 (standard for TOTP)
    Base32 base32 = new Base32();
    return base32.encodeAsString(randomBytes).replace("=", "");
  }

  @Override
  public String[] generateRecoveryCodes(int count) {
    String[] codes = new String[count];
    for (int i = 0; i < count; i++) {
      codes[i] = generateRecoveryCode();
    }
    return codes;
  }

  private String generateRecoveryCode() {
    // Format: XXXX-XXXX-XXXX-XXXX (4 groups of 10 alphanumeric chars)
    StringBuilder code = new StringBuilder();
    for (int group = 0; group < RECOVERY_CODE_GROUPS; group++) {
      if (group > 0) {
        code.append("-");
      }
      for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
        code.append(generateRandomChar());
      }
    }
    return code.toString();
  }

  private char generateRandomChar() {
    // Use alphanumeric characters (exclude confusing ones like 0, O, 1, I, l)
    String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    int index = secureRandom.nextInt(chars.length());
    return chars.charAt(index);
  }
}
