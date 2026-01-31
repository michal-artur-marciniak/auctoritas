package dev.auctoritas.auth.domain.model.project;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "project_settings")
@Getter
public class ProjectSettings extends BaseEntity {
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false, unique = true)
  private Project project;

  @Column(nullable = false)
  private int minLength = 8;

  @Column(nullable = false)
  private boolean requireUppercase = true;

  @Column(nullable = false)
  private boolean requireLowercase = true;

  @Column(nullable = false)
  private boolean requireNumbers = true;

  @Column(nullable = false)
  private boolean requireSpecialChars = false;

  @Column(nullable = false)
  private int passwordHistoryCount = 0;

  @Column(nullable = false)
  private int accessTokenTtlSeconds = 3600;

  @Column(nullable = false)
  private int refreshTokenTtlSeconds = 604800;

  @Column(nullable = false)
  private int maxSessions = 5;

  @Column(nullable = false)
  private int failedLoginMaxAttempts = 5;

  @Column(nullable = false)
  private int failedLoginWindowSeconds = 900;

  @Column(nullable = false)
  private boolean requireVerifiedEmailForLogin = false;

  @Column(nullable = false)
  private boolean mfaEnabled = false;

  @Column(nullable = false)
  private boolean mfaRequired = false;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "oauth_config", columnDefinition = "json", nullable = false)
  private Map<String, Object> oauthConfig = new HashMap<>();

  @Column(name = "oauth_google_client_secret_enc")
  private String oauthGoogleClientSecretEnc;

  @Column(name = "oauth_github_client_secret_enc")
  private String oauthGithubClientSecretEnc;

  @Column(name = "oauth_microsoft_client_secret_enc")
  private String oauthMicrosoftClientSecretEnc;

  @Column(name = "oauth_facebook_client_secret_enc")
  private String oauthFacebookClientSecretEnc;

  @Column(name = "oauth_apple_private_key_enc")
  private String oauthApplePrivateKeyEnc;

  protected ProjectSettings() {
  }

  void setProjectInternal(Project project) {
    this.project = project;
  }

  /**
   * Returns a defensive copy of OAuth config to prevent external modification.
   */
  public Map<String, Object> getOauthConfig() {
    return new HashMap<>(oauthConfig);
  }

  /**
   * Updates password policy settings.
   */
  public void updatePasswordPolicy(
      int minLength,
      boolean requireUppercase,
      boolean requireLowercase,
      boolean requireNumbers,
      boolean requireSpecialChars,
      int passwordHistoryCount) {

    if (minLength < 6 || minLength > 128) {
      throw new DomainValidationException("password_min_length_invalid");
    }
    if (passwordHistoryCount < 0 || passwordHistoryCount > 24) {
      throw new DomainValidationException("password_history_count_invalid");
    }

    this.minLength = minLength;
    this.requireUppercase = requireUppercase;
    this.requireLowercase = requireLowercase;
    this.requireNumbers = requireNumbers;
    this.requireSpecialChars = requireSpecialChars;
    this.passwordHistoryCount = passwordHistoryCount;
  }

  /**
   * Updates session settings.
   */
  public void updateSessionSettings(
      int accessTokenTtlSeconds,
      int refreshTokenTtlSeconds,
      int maxSessions) {

    if (accessTokenTtlSeconds < 60 || accessTokenTtlSeconds > 86400) {
      throw new DomainValidationException("access_token_ttl_invalid");
    }
    if (refreshTokenTtlSeconds < 300 || refreshTokenTtlSeconds > 2592000) {
      throw new DomainValidationException("refresh_token_ttl_invalid");
    }
    if (maxSessions < 1 || maxSessions > 100) {
      throw new DomainValidationException("max_sessions_invalid");
    }

    this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    this.maxSessions = maxSessions;
  }

  /**
   * Updates MFA settings.
   */
  public void updateMfaSettings(boolean enabled, boolean required) {
    if (required && !enabled) {
      throw new DomainValidationException("mfa_required_but_not_enabled");
    }
    this.mfaEnabled = enabled;
    this.mfaRequired = required;
  }

  /**
   * Updates authentication settings.
   */
  public void updateAuthSettings(boolean requireVerifiedEmailForLogin) {
    this.requireVerifiedEmailForLogin = requireVerifiedEmailForLogin;
  }

  /**
   * Updates OAuth configuration.
   */
  public void updateOauthConfig(Map<String, Object> config) {
    if (config == null) {
      this.oauthConfig = new HashMap<>();
    } else {
      this.oauthConfig = new HashMap<>(config);
    }
  }

  /**
   * Sets encrypted Google client secret.
   */
  public void setOauthGoogleClientSecretEnc(String secret) {
    this.oauthGoogleClientSecretEnc = secret;
  }

  /**
   * Sets encrypted GitHub client secret.
   */
  public void setOauthGithubClientSecretEnc(String secret) {
    this.oauthGithubClientSecretEnc = secret;
  }

  /**
   * Sets encrypted Microsoft client secret.
   */
  public void setOauthMicrosoftClientSecretEnc(String secret) {
    this.oauthMicrosoftClientSecretEnc = secret;
  }

  /**
   * Sets encrypted Facebook client secret.
   */
  public void setOauthFacebookClientSecretEnc(String secret) {
    this.oauthFacebookClientSecretEnc = secret;
  }

  /**
   * Sets encrypted Apple private key.
   */
  public void setOauthApplePrivateKeyEnc(String key) {
    this.oauthApplePrivateKeyEnc = key;
  }
}
