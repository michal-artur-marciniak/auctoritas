package dev.auctoritas.auth.infrastructure.config;

import dev.auctoritas.auth.adapter.out.security.EncryptionAdapter;
import dev.auctoritas.auth.application.port.out.security.EncryptionPort;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MFA encryption using AES-256-GCM.
 */
@Configuration
@EnableConfigurationProperties(MfaEncryptionProperties.class)
public class MfaCryptoConfig {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12; // 96 bits
  private static final int GCM_TAG_LENGTH = 128; // bits
  private static final int AES_KEY_SIZE = 256; // bits

  @Bean
  public EncryptionPort mfaEncryptionPort(MfaEncryptionProperties properties) {
    SecretKey key = loadOrGenerateKey(properties);
    return new EncryptionAdapter(key, GCM_IV_LENGTH, GCM_TAG_LENGTH);
  }

  private SecretKey loadOrGenerateKey(MfaEncryptionProperties properties) {
    String keyBase64 = properties.key();

    if (keyBase64 != null && !keyBase64.trim().isEmpty()) {
      // Use provided key
      byte[] decoded = Base64.getDecoder().decode(keyBase64.trim());
      if (decoded.length != 32) {
        throw new IllegalStateException(
            "MFA encryption key must be 32 bytes (256 bits) when Base64 decoded.");
      }
      return new SecretKeySpec(decoded, ALGORITHM);
    }

    // Generate a random key (for development only - in production, key should be provided)
    try {
      KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
      keyGen.init(AES_KEY_SIZE, new SecureRandom());
      SecretKey key = keyGen.generateKey();

      // Log the generated key so it can be saved for future use
      String generatedKey = Base64.getEncoder().encodeToString(key.getEncoded());
      System.out.println("WARNING: Generated random MFA encryption key. For production, set "
          + "auctoritas.auth.mfa.encryption.key=" + generatedKey);

      return key;
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("AES algorithm not available", e);
    }
  }
}
