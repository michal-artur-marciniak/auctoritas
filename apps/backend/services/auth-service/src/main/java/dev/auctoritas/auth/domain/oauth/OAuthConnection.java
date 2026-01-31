package dev.auctoritas.auth.domain.oauth;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.shared.persistence.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Rich domain entity representing an OAuth connection.
 * Links an end-user account to an external OAuth provider identity.
 */
@Entity
@Table(
    name = "oauth_connections",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"project_id", "provider", "provider_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthConnection extends BaseAuditEntity {

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private EndUser user;

  @Column(nullable = false, length = 50)
  private String provider;

  @Column(name = "provider_user_id", nullable = false, length = 255)
  private String providerUserId;

  @Column(nullable = false, length = 255)
  private String email;

  /**
   * Establishes a new OAuth connection for the specified user.
   *
   * @param project the project context
   * @param user the end user to link
   * @param provider the OAuth provider (e.g., "google", "github")
   * @param providerUserId the provider's user identifier
   * @param email the email address from the OAuth provider
   * @return a new OAuthConnection instance
   * @throws IllegalArgumentException if any required parameter is null
   */
  public static OAuthConnection establish(
      Project project,
      EndUser user,
      String provider,
      String providerUserId,
      String email) {
    Objects.requireNonNull(project, "project_required");
    Objects.requireNonNull(user, "user_required");
    Objects.requireNonNull(provider, "provider_required");
    Objects.requireNonNull(providerUserId, "provider_user_id_required");
    Objects.requireNonNull(email, "email_required");

    OAuthConnection connection = new OAuthConnection();
    connection.project = project;
    connection.user = user;
    connection.provider = provider;
    connection.providerUserId = providerUserId;
    connection.email = email;

    connection.registerEvent(
        new OAuthConnectionEstablishedEvent(
            UUID.randomUUID(),
            connection.getId(),
            user.getId(),
            project.getId(),
            provider,
            providerUserId,
            Instant.now()));

    return connection;
  }

  /**
   * Updates the email address associated with this connection.
   *
   * @param newEmail the new email address
   * @throws IllegalArgumentException if newEmail is null
   */
  public void updateEmail(String newEmail) {
    Objects.requireNonNull(newEmail, "email_required");
    
    if (this.email.equals(newEmail)) {
      return; // No change needed
    }

    String oldEmail = this.email;
    this.email = newEmail;

    registerEvent(
        new OAuthConnectionEmailUpdatedEvent(
            UUID.randomUUID(),
            getId(),
            user.getId(),
            oldEmail,
            newEmail,
            Instant.now()));
  }

  /**
   * Checks if this connection belongs to the specified user.
   *
   * @param userId the user ID to check
   * @return true if connection belongs to user
   */
  public boolean belongsTo(UUID userId) {
    return user != null && user.getId().equals(userId);
  }

  /**
   * Checks if this connection is for the specified provider.
   *
   * @param providerName the provider name to check
   * @return true if connection is for the specified provider
   */
  public boolean isForProvider(String providerName) {
    return provider.equalsIgnoreCase(providerName);
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
}
