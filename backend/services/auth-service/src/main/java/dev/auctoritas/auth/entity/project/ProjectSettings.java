package dev.auctoritas.auth.entity.project;

import dev.auctoritas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "project_settings")
@Getter
@Setter
@NoArgsConstructor
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

  /** Encrypted at rest; never exposed directly via API responses. */
  @Column(name = "oauth_google_client_secret_enc")
  private String oauthGoogleClientSecretEnc;

  /** Encrypted at rest; never exposed directly via API responses. */
  @Column(name = "oauth_github_client_secret_enc")
  private String oauthGithubClientSecretEnc;

  /** Encrypted at rest; never exposed directly via API responses. */
  @Column(name = "oauth_microsoft_client_secret_enc")
  private String oauthMicrosoftClientSecretEnc;

  /** Encrypted at rest; never exposed directly via API responses. */
  @Column(name = "oauth_facebook_client_secret_enc")
  private String oauthFacebookClientSecretEnc;
}
