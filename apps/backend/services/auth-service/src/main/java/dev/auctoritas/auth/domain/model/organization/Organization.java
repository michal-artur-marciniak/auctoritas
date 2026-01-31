package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.domain.event.DomainEvent;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.organization.OrganizationStatus;
import dev.auctoritas.auth.domain.model.project.Slug;
import dev.auctoritas.auth.shared.persistence.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * Aggregate root representing an organization.
 */
@Entity
@Table(name = "organizations")
@Getter
public class Organization extends BaseAuditEntity {
  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, unique = true, length = 50)
  private String slug;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrganizationStatus status = OrganizationStatus.ACTIVE;

  @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrganizationMember> members = new ArrayList<>();

  @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrganizationInvitation> invitations = new ArrayList<>();

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  protected Organization() {
  }

  /**
   * Factory method to create a new Organization with validated data.
   * Publishes OrganizationCreatedEvent.
   */
  public static Organization create(String name, Slug slug) {
    Organization organization = new Organization();
    organization.setName(name);
    organization.slug = slug.value();
    organization.status = OrganizationStatus.ACTIVE;

    organization.registerEvent(new OrganizationCreatedEvent(
        UUID.randomUUID(),
        organization.getId(),
        organization.name,
        organization.slug,
        Instant.now()
    ));

    return organization;
  }

  private void setName(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DomainValidationException("organization_name_required");
    }
    String trimmed = name.trim();
    if (trimmed.length() > 100) {
      throw new DomainValidationException("organization_name_too_long");
    }
    this.name = trimmed;
  }

  /**
   * Renames the organization.
   */
  public void rename(String newName) {
    ensureNotDeleted();
    setName(newName);
  }

  /**
   * Changes the slug.
   */
  public void changeSlug(Slug newSlug) {
    ensureNotDeleted();
    this.slug = newSlug.value();
  }

  /**
   * Suspends the organization.
   */
  public void suspend() {
    ensureNotDeleted();
    if (this.status == OrganizationStatus.SUSPENDED) {
      throw new DomainValidationException("organization_already_suspended");
    }
    this.status = OrganizationStatus.SUSPENDED;
  }

  /**
   * Reactivates a suspended organization.
   */
  public void reactivate() {
    ensureNotDeleted();
    if (this.status != OrganizationStatus.SUSPENDED) {
      throw new DomainValidationException("organization_not_suspended");
    }
    this.status = OrganizationStatus.ACTIVE;
  }

  /**
   * Marks the organization as deleted.
   */
  public void markDeleted() {
    if (this.status == OrganizationStatus.DELETE) {
      throw new DomainValidationException("organization_already_deleted");
    }
    this.status = OrganizationStatus.DELETE;
  }

  /**
   * Checks if the organization is active.
   */
  public boolean isActive() {
    return this.status == OrganizationStatus.ACTIVE;
  }

  /**
   * Checks if the organization is deleted.
   */
  public boolean isDeleted() {
    return this.status == OrganizationStatus.DELETE;
  }

  private void ensureNotDeleted() {
    if (isDeleted()) {
      throw new DomainValidationException("organization_deleted");
    }
  }

  /**
   * Adds a member to this organization.
   */
  public void addMember(OrganizationMember member) {
    ensureNotDeleted();
    if (member.getOrganization() != null && !member.getOrganization().equals(this)) {
      throw new DomainValidationException("member_belongs_to_different_organization");
    }
    member.setOrganization(this);
    this.members.add(member);
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
