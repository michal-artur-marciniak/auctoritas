package dev.auctoritas.auth.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
@EnableConfigurationProperties(OAuthEncryptionProperties.class)
public class OAuthCryptoConfig {

  @Bean
  public TextEncryptor oauthClientSecretEncryptor(OAuthEncryptionProperties properties) {
    String key = properties.key();
    String salt = properties.salt();

    if (key == null || key.trim().isEmpty()) {
      throw new IllegalStateException(
          "OAuth encryption key is required (auctoritas.auth.oauth.encryption.key).");
    }
    if (salt == null || !salt.matches("[0-9a-fA-F]{16}")) {
      throw new IllegalStateException(
          "OAuth encryption salt must be 16 hex chars (auctoritas.auth.oauth.encryption.salt).");
    }

    return Encryptors.delux(key.trim(), salt.toLowerCase());
  }
}
