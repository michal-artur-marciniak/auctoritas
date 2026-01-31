package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.domain.event.DomainEvent;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.organization.OrganizationMemberRole;
import dev.auctoritas.auth.domain.model.organization.OrganizationMemberStatus;
import dev.auctoritas.auth.domain.model.enduser.Email;
import dev.auctoritas.auth.shared.persistence.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
 * Entity representing a member of an organization.
 * Part of the Organization aggregate.
 */
@Entity
@Table(
    name = "organization_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "email"}))
@Getter
public class OrganizationMember extends BaseAuditEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(length = 100)
  private String name;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrganizationMemberRole role;

  @Column(name = "email_verified", nullable = false)
  private Boolean emailVerified = false;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrganizationMemberStatus status = OrganizationMemberStatus.ACTIVE;

  @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
  private OrganizationMemberMfa mfa;

  @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrganizationMemberSession> sessions = new ArrayList<>();

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  protected OrganizationMember() {
  }

  /**
   * Factory method to create a new OrganizationMember with validated data.
   * Publishes OrganizationMemberCreatedEvent.
   */
  public static OrganizationMember create(
      Organization organization,
      Email email,
      String passwordHash,
      String name,
      OrganizationMemberRole role,
      boolean emailVerified) {

    if (organization == null) {
      throw new DomainValidationException("organization_required");
    }
    if (email == null) {
      throw new DomainValidationException("email_required");
    }
    if (passwordHash == null || passwordHash.isEmpty()) {
      throw new DomainValidationException("password_hash_required");
    }
    if (role == null) {
      throw new DomainValidationException("role_required");
    }

    OrganizationMember member = new OrganizationMember();
    member.organization = organization;
    member.email = email.value();
    member.passwordHash = passwordHash;
    member.name = normalizeName(name);
    member.role = role;
    member.emailVerified = emailVerified;
    member.status = OrganizationMemberStatus.ACTIVE;

    // Register domain event
    member.registerEvent(new OrganizationMemberCreatedEvent(
        UUID.randomUUID(),
        member.getId(),
        organization.getId(),
        member.email,
        member.name,
        member.role,
        member.emailVerified,
        Instant.now()
    ));

    return member;
  }

  private static String normalizeName(String name) {
    if (name == null) {
      return null;
    }
    String trimmed = name.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * Sets the organization. Used internally by Organization.addMember().
   */
  void setOrganization(Organization organization) {
    this.organization = organization;
  }

  /**
   * Updates the member's name.
   */
  public void updateName(String newName) {
    ensureActive();
    this.name = normalizeName(newName);
  }

  /**
   * Updates the member's avatar URL.
   */
  public void updateAvatarUrl(String avatarUrl) {
    ensureActive();
    this.avatarUrl = avatarUrl;
  }

  /**
   * Verifies the member's email address.
   */
  public void verifyEmail() {
    ensureActive();
    this.emailVerified = true;
  }

  /**
   * Changes the member's role.
   */
  public void changeRole(OrganizationMemberRole newRole) {
    ensureActive();
    if (newRole == null) {
      throw new DomainValidationException("role_required");
    }
    this.role = newRole;
  }

  /**
   * Updates the password hash.
   */
  public void updatePasswordHash(String newPasswordHash) {
    ensureActive();
    if (newPasswordHash == null || newPasswordHash.isEmpty()) {
      throw new DomainValidationException("password_hash_required");
    }
    this.passwordHash = newPasswordHash;
  }

  /**
   * Suspends the member.
   */
  public void suspend() {
    if (this.status == OrganizationMemberStatus.SUSPENDED) {
      throw new DomainValidationException("member_already_suspended");
    }
    this.status = OrganizationMemberStatus.SUSPENDED;
  }

  /**
   * Reactivates a suspended member.
   */
  public void reactivate() {
    if (this.status != OrganizationMemberStatus.SUSPENDED) {
      throw new DomainValidationException("member_not_suspended");
    }
    this.status = OrganizationMemberStatus.ACTIVE;
  }

  /**
   * Checks if the member is active.
   */
  public boolean isActive() {
    return this.status == OrganizationMemberStatus.ACTIVE;
  }

  private void ensureActive() {
    if (!isActive()) {
      throw new DomainValidationException("member_not_active");
    }
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
