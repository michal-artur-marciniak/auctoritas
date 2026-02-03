package dev.auctoritas.auth.infrastructure.config;

import dev.auctoritas.auth.adapter.out.security.EncryptionAdapter;
import dev.auctoritas.auth.application.port.out.security.EncryptionPort;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final int GCM_IV_LENGTH = 12; // 96 bits
  private static final int GCM_TAG_LENGTH = 128; // bits
  private static final Logger log = LoggerFactory.getLogger(MfaCryptoConfig.class);

  @Bean
  public EncryptionPort mfaEncryptionPort(MfaEncryptionProperties properties) {
    SecretKey key = loadOrGenerateKey(properties);
    return new EncryptionAdapter(key, GCM_IV_LENGTH, GCM_TAG_LENGTH);
  }

  private SecretKey loadOrGenerateKey(MfaEncryptionProperties properties) {
    String keyBase64 = properties.key();
    if (keyBase64 == null || keyBase64.trim().isEmpty()) {
      throw new IllegalStateException("MFA encryption key is required (auctoritas.auth.mfa.encryption.key)");
    }

    byte[] decoded = Base64.getDecoder().decode(keyBase64.trim());
    if (decoded.length != 32) {
      throw new IllegalStateException(
          "auctoritas.auth.mfa.encryption.key must decode to 32 bytes (256 bits), got " + decoded.length);
    }

    log.info("Loaded MFA encryption key for AES-256-GCM");
    return new SecretKeySpec(decoded, ALGORITHM);
  }
}
