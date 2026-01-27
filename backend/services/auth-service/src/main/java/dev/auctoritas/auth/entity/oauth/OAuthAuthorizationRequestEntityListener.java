package dev.auctoritas.auth.entity.oauth;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class OAuthAuthorizationRequestEntityListener {

  private static volatile TextEncryptor oauthClientSecretEncryptor;

  @Autowired
  void setOauthClientSecretEncryptor(
      @Qualifier("oauthClientSecretEncryptor") TextEncryptor encryptor) {
    oauthClientSecretEncryptor = encryptor;
  }

  @PrePersist
  @PreUpdate
  public void encryptCodeVerifier(OAuthAuthorizationRequest entity) {
    if (entity == null) {
      return;
    }

    TextEncryptor encryptor = oauthClientSecretEncryptor;
    if (encryptor == null) {
      throw new IllegalStateException("oauthClientSecretEncryptor not initialized");
    }

    String raw = entity.getCodeVerifier();
    if (raw == null) {
      return;
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      return;
    }

    // Avoid double-encrypting on updates; Encryptors.delux produces hex ciphertext.
    if (isLikelyEncrypted(trimmed)) {
      if (!trimmed.equals(raw)) {
        entity.setCodeVerifier(trimmed);
      }
      return;
    }

    entity.setCodeVerifier(encryptor.encrypt(trimmed));
  }

  private static boolean isLikelyEncrypted(String value) {
    if (value == null) {
      return false;
    }
    int len = value.length();
    if (len < 32 || (len % 2) != 0) {
      return false;
    }
    for (int i = 0; i < len; i++) {
      char c = value.charAt(i);
      boolean hex =
          (c >= '0' && c <= '9')
              || (c >= 'a' && c <= 'f')
              || (c >= 'A' && c <= 'F');
      if (!hex) {
        return false;
      }
    }
    return true;
  }
}
