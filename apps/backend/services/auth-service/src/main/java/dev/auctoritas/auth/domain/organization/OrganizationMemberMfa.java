package dev.auctoritas.auth.domain.organization;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.mfa.MfaDisabledEvent;
import dev.auctoritas.auth.domain.mfa.MfaEnabledEvent;
import dev.auctoritas.auth.domain.mfa.MfaSetupInitiatedEvent;
import dev.auctoritas.auth.domain.mfa.TotpSecret;
import dev.auctoritas.auth.domain.shared.DomainEvent;
import dev.auctoritas.auth.shared.persistence.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
 * Aggregate root for organization member MFA settings.
 * Manages TOTP secret, enabled status, and verification state.
 */
@Entity
@Table(
    name = "org_member_mfa",
    uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "organization_id"}))
@Getter
public class OrganizationMemberMfa extends BaseAuditEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private OrganizationMember member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(name = "encrypted_secret", nullable = false, length = 500)
  private String encryptedSecret;

  @Column(nullable = false)
  private Boolean enabled = false;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  protected OrganizationMemberMfa() {
  }

  /**
   * Factory method to create a new OrganizationMemberMfa with validated data.
   * MFA is initially disabled and must be verified before enabling.
   * Publishes MfaSetupInitiatedEvent.
   *
   * @param member the organization member
   * @param organization the organization
   * @param secret the encrypted TOTP secret
   * @return new OrganizationMemberMfa instance
   * @throws DomainValidationException if any parameter is invalid
   */
  public static OrganizationMemberMfa create(OrganizationMember member, Organization organization, TotpSecret secret) {
    if (member == null) {
      throw new DomainValidationException("member_required");
    }
    if (organization == null) {
      throw new DomainValidationException("organization_required");
    }
    if (secret == null) {
      throw new DomainValidationException("totp_secret_required");
    }

    OrganizationMemberMfa mfa = new OrganizationMemberMfa();
    mfa.setId(UUID.randomUUID());
    mfa.member = member;
    mfa.organization = organization;
    mfa.encryptedSecret = secret.encryptedValue();
    mfa.enabled = false;
    mfa.verifiedAt = null;

    // Register domain event
    mfa.registerEvent(new MfaSetupInitiatedEvent(
        UUID.randomUUID(),
        mfa.getId(),
        member.getId(),
        organization.getId(),
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
        this.member.getId(),
        this.organization.getId(),
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
        this.member.getId(),
        this.organization.getId(),
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
