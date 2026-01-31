package dev.auctoritas.auth.domain.oauth;

import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.shared.persistence.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import dev.auctoritas.auth.infrastructure.persistence.OAuthAuthorizationRequestEntityListener;

@Entity
@Table(
    name = "oauth_authorization_requests",
    uniqueConstraints = @UniqueConstraint(columnNames = {"state_hash"}))
@EntityListeners(OAuthAuthorizationRequestEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class OAuthAuthorizationRequest extends BaseAuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(nullable = false, length = 50)
  private String provider;

  @Column(name = "state_hash", nullable = false, length = 128)
  private String stateHash;

  @Column(name = "code_verifier", nullable = false, length = 256)
  private String codeVerifier;

  @Column(name = "app_redirect_uri", nullable = false, length = 2000)
  private String appRedirectUri;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  /** Returns the PKCE code verifier decrypted using the provided encryptor. */
  public String getCodeVerifierDecrypted(TextEncryptor oauthClientSecretEncryptor) {
    if (codeVerifier == null || codeVerifier.isBlank()) {
      return codeVerifier;
    }
    String trimmed = codeVerifier.trim();
    if (!trimmed.startsWith(OAuthAuthorizationRequestEntityListener.ENC_PREFIX)) {
      throw new IllegalStateException("codeVerifier is not encrypted");
    }
    String ciphertext =
        trimmed.substring(OAuthAuthorizationRequestEntityListener.ENC_PREFIX.length());
    return oauthClientSecretEncryptor.decrypt(ciphertext);
  }
}
