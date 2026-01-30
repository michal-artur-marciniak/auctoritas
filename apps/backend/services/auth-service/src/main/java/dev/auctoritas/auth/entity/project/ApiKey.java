package dev.auctoritas.auth.entity.project;

import dev.auctoritas.auth.shared.persistence.BaseAuditEntity;
import dev.auctoritas.auth.domain.apikey.ApiKeyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing an API key issued for a project.
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
public class ApiKey extends BaseAuditEntity {

  private static final int NAME_MAX_LENGTH = 50;
  private static final int PREFIX_MAX_LENGTH = 10;
  private static final int KEY_HASH_MAX_LENGTH = 64;

  /**
   * Creates a new API key enforcing required invariants.
   *
   * @param project owning project
   * @param name display name for the key
   * @param prefix prefix for the raw key
   * @param keyHash hashed key material
   * @return new API key instance
   */
  public static ApiKey create(Project project, String name, String prefix, String keyHash) {
    ApiKey apiKey = new ApiKey();
    apiKey.setProject(requireProject(project));
    apiKey.setName(requireValue("name", name, NAME_MAX_LENGTH));
    apiKey.setPrefix(requireValue("prefix", prefix, PREFIX_MAX_LENGTH));
    apiKey.setKeyHash(requireValue("keyHash", keyHash, KEY_HASH_MAX_LENGTH));
    apiKey.setStatus(ApiKeyStatus.ACTIVE);
    return apiKey;
  }

  private static Project requireProject(Project project) {
    if (project == null) {
      throw new IllegalArgumentException("project is required");
    }
    return project;
  }

  private static String requireValue(String field, String value, int maxLength) {
    if (value == null) {
      throw new IllegalArgumentException(field + " is required");
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(field + " is required");
    }
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(field + " exceeds max length " + maxLength);
    }
    return trimmed;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(nullable = false, length = 10)
  private String prefix;

  @Column(nullable = false, length = 64)
  private String keyHash;

  @Column(nullable = true)
  private LocalDateTime lastUsedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ApiKeyStatus status = ApiKeyStatus.ACTIVE;
}
