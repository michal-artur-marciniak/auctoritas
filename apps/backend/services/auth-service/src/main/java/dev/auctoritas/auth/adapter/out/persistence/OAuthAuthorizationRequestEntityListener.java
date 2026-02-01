package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.domain.oauth.OAuthAuthorizationRequest;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class OAuthAuthorizationRequestEntityListener {

  private static final String ENC_PREFIX = "ENC:";

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

    // Avoid double-encrypting on updates.
    if (isEncryptedValue(trimmed)) {
      if (!trimmed.equals(raw)) {
        entity.setCodeVerifier(trimmed);
      }
      return;
    }

    entity.setCodeVerifier(ENC_PREFIX + encryptor.encrypt(trimmed));
  }

  private static boolean isEncryptedValue(String value) {
    return value != null && value.startsWith(ENC_PREFIX);
  }
}
