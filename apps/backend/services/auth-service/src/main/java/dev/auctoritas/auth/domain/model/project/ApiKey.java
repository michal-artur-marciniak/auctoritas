package dev.auctoritas.auth.domain.model.project;

import dev.auctoritas.auth.domain.apikey.ApiKeyStatus;
import dev.auctoritas.auth.domain.event.DomainEvent;
import dev.auctoritas.auth.shared.persistence.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Rich domain entity representing an API key issued for a project.
 */
@Entity
@Table(name = "api_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiKey extends BaseAuditEntity {

  private static final int NAME_MAX_LENGTH = 50;
  private static final int PREFIX_MAX_LENGTH = 10;
  private static final int KEY_HASH_MAX_LENGTH = 64;

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

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

  /**
   * Creates a new API key enforcing required invariants.
   *
   * @param project owning project
   * @param name display name for the key
   * @param prefix prefix for the raw key
   * @param keyHash hashed key material
   * @return new API key instance
   * @throws IllegalArgumentException if any required parameter is invalid
   */
  public static ApiKey create(Project project, String name, String prefix, String keyHash) {
    ApiKey apiKey = new ApiKey();
    apiKey.project = requireProject(project);
    apiKey.name = requireValue("name", name, NAME_MAX_LENGTH);
    apiKey.prefix = requireValue("prefix", prefix, PREFIX_MAX_LENGTH);
    apiKey.keyHash = requireValue("keyHash", keyHash, KEY_HASH_MAX_LENGTH);
    apiKey.status = ApiKeyStatus.ACTIVE;
    apiKey.lastUsedAt = null;

    apiKey.registerEvent(
        new ApiKeyCreatedEvent(
            UUID.randomUUID(),
            apiKey.getId(),
            project.getId(),
            apiKey.name,
            Instant.now()));

    return apiKey;
  }

  /**
   * Records that the API key was used.
   * Updates lastUsedAt and publishes a usage event.
   */
  public void recordUsage() {
    validateActive();
    this.lastUsedAt = LocalDateTime.now();

    registerEvent(
        new ApiKeyUsedEvent(
            UUID.randomUUID(),
            getId(),
            project.getId(),
            Instant.now()));
  }

  /**
   * Revokes this API key.
   *
   * @param reason the reason for revocation (can be null)
   * @throws IllegalStateException if key is already revoked
   */
  public void revoke(String reason) {
    if (this.status == ApiKeyStatus.REVOKED) {
      throw new IllegalStateException("api_key_already_revoked");
    }

    this.status = ApiKeyStatus.REVOKED;

    registerEvent(
        new ApiKeyRevokedEvent(
            UUID.randomUUID(),
            getId(),
            project.getId(),
            reason != null ? reason : "manual_revocation",
            Instant.now()));
  }

  /**
   * Validates that the API key is active and can be used.
   *
   * @throws IllegalStateException if key is revoked
   */
  public void validateForUse() {
    validateActive();
  }

  /**
   * Checks if the API key belongs to the specified project.
   *
   * @param projectId the project ID to check
   * @return true if key belongs to project
   */
  public boolean belongsTo(UUID projectId) {
    return project != null && project.getId().equals(projectId);
  }

  /**
   * Checks if the API key is active.
   *
   * @return true if status is ACTIVE
   */
  public boolean isActive() {
    return status == ApiKeyStatus.ACTIVE;
  }

  /**
   * Returns an unmodifiable view of domain events.
   *
   * @return list of domain events
   */
  public List<DomainEvent> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
  }

  /**
   * Clears all domain events. Should be called after events are published.
   */
  public void clearDomainEvents() {
    domainEvents.clear();
  }

  private void registerEvent(DomainEvent event) {
    domainEvents.add(event);
  }

  private void validateActive() {
    if (status != ApiKeyStatus.ACTIVE) {
      throw new IllegalStateException("api_key_revoked");
    }
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
}
