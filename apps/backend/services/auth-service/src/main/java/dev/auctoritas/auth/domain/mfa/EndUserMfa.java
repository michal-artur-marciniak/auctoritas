package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.shared.DomainEvent;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
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
import java.util.UUID;
import lombok.Getter;

/**
 * Aggregate root for end-user MFA settings.
 * Manages TOTP secret, enabled status, and verification state.
 */
@Entity
@Table(
    name = "user_mfa",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "project_id"}))
@Getter
public class EndUserMfa extends BaseAuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private EndUser user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(name = "encrypted_secret", nullable = false, length = 500)
  private String encryptedSecret;

  @Column(nullable = false)
  private Boolean enabled = false;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  protected EndUserMfa() {
  }

  /**
   * Factory method to create a new EndUserMfa with validated data.
   * MFA is initially disabled and must be verified before enabling.
   * Publishes MfaSetupInitiatedEvent.
   *
   * @param user the end user
   * @param project the project
   * @param secret the encrypted TOTP secret
   * @return new EndUserMfa instance
   * @throws DomainValidationException if any parameter is invalid
   */
  public static EndUserMfa create(EndUser user, Project project, TotpSecret secret) {
    if (user == null) {
      throw new DomainValidationException("user_required");
    }
    if (project == null) {
      throw new DomainValidationException("project_required");
    }
    if (secret == null) {
      throw new DomainValidationException("totp_secret_required");
    }

    EndUserMfa mfa = new EndUserMfa();
    mfa.user = user;
    mfa.project = project;
    mfa.encryptedSecret = secret.encryptedValue();
    mfa.enabled = false;
    mfa.verifiedAt = null;

    // Register domain event
    mfa.registerEvent(new MfaSetupInitiatedEvent(
        UUID.randomUUID(),
        mfa.getId(),
        user.getId(),
        project.getId(),
        Instant.now()
    ));

    return mfa;
  }

  /**
   * Enables MFA after successful verification.
   * Can only be called if MFA is not already enabled.
   * Publishes MfaEnabledEvent.
   *
   * @throws DomainValidationException if MFA is already enabled
   */
  public void enable() {
    if (Boolean.TRUE.equals(this.enabled)) {
      throw new DomainValidationException("mfa_already_enabled");
    }

    this.enabled = true;
    this.verifiedAt = Instant.now();

    registerEvent(new MfaEnabledEvent(
        UUID.randomUUID(),
        this.getId(),
        this.user.getId(),
        this.project.getId(),
        "TOTP",
        Instant.now()
    ));
  }

  /**
   * Disables MFA.
   * Publishes MfaDisabledEvent.
   *
   * @param reason the reason for disabling MFA
   * @throws DomainValidationException if MFA is not enabled
   */
  public void disable(String reason) {
    if (!Boolean.TRUE.equals(this.enabled)) {
      throw new DomainValidationException("mfa_not_enabled");
    }

    this.enabled = false;
    this.verifiedAt = null;

    registerEvent(new MfaDisabledEvent(
        UUID.randomUUID(),
        this.getId(),
        this.user.getId(),
        this.project.getId(),
        reason,
        Instant.now()
    ));
  }

  /**
   * Checks if MFA is enabled and verified.
   */
  public boolean isEnabled() {
    return Boolean.TRUE.equals(this.enabled);
  }

  /**
   * Gets the TOTP secret as a value object.
   */
  public TotpSecret getSecret() {
    return TotpSecret.of(this.encryptedSecret);
  }

  /**
   * Registers a domain event.
   */
  protected void registerEvent(DomainEvent event) {
    domainEvents.add(event);
  }

  /**
   * Returns unmodifiable list of domain events.
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
}
