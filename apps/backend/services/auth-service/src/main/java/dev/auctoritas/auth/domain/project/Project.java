package dev.auctoritas.auth.domain.project;

import dev.auctoritas.auth.domain.shared.DomainEvent;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.ProjectStatus;
import dev.auctoritas.auth.domain.project.Slug;
import dev.auctoritas.auth.shared.persistence.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Aggregate root representing a project in the auth domain.
 */
@Entity
@Table(
    name = "projects",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "slug"}))
@Getter
public class Project extends BaseAuditEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 50)
  private String slug;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProjectStatus status = ProjectStatus.ACTIVE;

  @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private ProjectSettings settings;

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  protected Project() {
  }

  /**
   * Factory method to create a new Project.
   * Publishes ProjectCreatedEvent.
   */
  public static Project create(Organization organization, String name, Slug slug) {
    if (organization == null) {
      throw new DomainValidationException("organization_required");
    }

    Project project = new Project();
    project.organization = organization;
    project.setName(name);
    project.slug = slug.value();
    project.status = ProjectStatus.ACTIVE;

    ProjectSettings settings = new ProjectSettings();
    settings.setProjectInternal(project);
    project.settings = settings;

    // Register domain event
    project.registerEvent(new ProjectCreatedEvent(
        UUID.randomUUID(),
        project.getId(),
        organization.getId(),
        project.name,
        project.slug,
        Instant.now()
    ));

    return project;
  }

  private void setName(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DomainValidationException("project_name_required");
    }
    String trimmed = name.trim();
    if (trimmed.length() > 100) {
      throw new DomainValidationException("project_name_too_long");
    }
    this.name = trimmed;
  }

  /**
   * Renames the project.
   * Publishes ProjectRenamedEvent.
   */
  public void rename(String newName) {
    ensureNotDeleted();
    String oldName = this.name;
    setName(newName);
    
    registerEvent(new ProjectRenamedEvent(
        UUID.randomUUID(),
        getId(),
        oldName,
        this.name,
        Instant.now()
    ));
  }

  /**
   * Changes the slug.
   */
  public void changeSlug(Slug newSlug) {
    ensureNotDeleted();
    this.slug = newSlug.value();
  }

  /**
   * Archives the project.
   * Publishes ProjectArchivedEvent.
   */
  public void archive() {
    ensureNotDeleted();
    this.status = ProjectStatus.ARCHIVED;
    
    registerEvent(new ProjectStatusChangedEvent(
        UUID.randomUUID(),
        getId(),
        ProjectStatus.ACTIVE,
        ProjectStatus.ARCHIVED,
        Instant.now()
    ));
  }

  /**
   * Suspends the project.
   * Publishes ProjectSuspendedEvent.
   */
  public void suspend() {
    ensureNotDeleted();
    this.status = ProjectStatus.SUSPENDED;
    
    registerEvent(new ProjectStatusChangedEvent(
        UUID.randomUUID(),
        getId(),
        ProjectStatus.ACTIVE,
        ProjectStatus.SUSPENDED,
        Instant.now()
    ));
  }

  /**
   * Reactivates an archived or suspended project.
   */
  public void reactivate() {
    ensureNotDeleted();
    ProjectStatus previousStatus = this.status;
    if (status == ProjectStatus.ACTIVE) {
      throw new DomainValidationException("project_already_active");
    }
    this.status = ProjectStatus.ACTIVE;
    
    registerEvent(new ProjectStatusChangedEvent(
        UUID.randomUUID(),
        getId(),
        previousStatus,
        ProjectStatus.ACTIVE,
        Instant.now()
    ));
  }

  /**
   * Marks the project as deleted.
   * Publishes ProjectDeletedEvent.
   */
  public void markDeleted() {
    if (status == ProjectStatus.DELETED) {
      throw new DomainValidationException("project_already_deleted");
    }
    this.status = ProjectStatus.DELETED;
    
    registerEvent(new ProjectDeletedEvent(
        UUID.randomUUID(),
        getId(),
        Instant.now()
    ));
  }

  /**
   * Checks if the project is deleted.
   */
  public boolean isDeleted() {
    return status == ProjectStatus.DELETED;
  }

  /**
   * Checks if the project is active.
   */
  public boolean isActive() {
    return status == ProjectStatus.ACTIVE;
  }

  private void ensureNotDeleted() {
    if (status == ProjectStatus.DELETED) {
      throw new DomainValidationException("project_deleted");
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
