package dev.auctoritas.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

class OAuthCryptoConfigTest {

  @Test
  void encryptsAndDecrypts() {
    OAuthCryptoConfig config = new OAuthCryptoConfig();
    TextEncryptor encryptor =
        config.oauthClientSecretEncryptor(
            new OAuthEncryptionProperties("test-key", "0123456789abcdef"));

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
                    new OAuthEncryptionProperties("test-key", "not-hex")))
        .isInstanceOf(IllegalStateException.class);
  }
}
