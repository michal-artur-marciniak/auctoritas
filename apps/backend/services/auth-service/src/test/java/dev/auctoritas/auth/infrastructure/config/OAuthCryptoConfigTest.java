package dev.auctoritas.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

class OAuthCryptoConfigTest {

  private static final String TEST_ENCRYPTION_KEY = "test-key"; // gitleaks:allow

  @Test
  void encryptsAndDecrypts() {
    OAuthCryptoConfig config = new OAuthCryptoConfig();
    TextEncryptor encryptor =
        config.oauthClientSecretEncryptor(
            new OAuthEncryptionProperties(TEST_ENCRYPTION_KEY, "0123456789abcdef"));

    String ciphertext = encryptor.encrypt("super-secret");
    assertThat(ciphertext).isNotEqualTo("super-secret");
    assertThat(encryptor.decrypt(ciphertext)).isEqualTo("super-secret");
  }

  @Test
  void rejectsInvalidSalt() {
    OAuthCryptoConfig config = new OAuthCryptoConfig();
    assertThatThrownBy(
            () ->
                config.oauthClientSecretEncryptor(
                    new OAuthEncryptionProperties(TEST_ENCRYPTION_KEY, "not-hex")))
        .isInstanceOf(IllegalStateException.class);
  }
}
