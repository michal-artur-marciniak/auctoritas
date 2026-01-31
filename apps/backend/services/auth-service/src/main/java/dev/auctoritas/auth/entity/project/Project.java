package dev.auctoritas.auth.entity.project;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.ProjectStatus;
import dev.auctoritas.auth.domain.valueobject.Slug;
import dev.auctoritas.auth.entity.organization.Organization;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

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

  protected Project() {
  }

  /**
   * Factory method to create a new Project.
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
   * Archives the project.
   */
  public void archive() {
    ensureNotDeleted();
    this.status = ProjectStatus.ARCHIVED;
  }

  /**
   * Suspends the project.
   */
  public void suspend() {
    ensureNotDeleted();
    this.status = ProjectStatus.SUSPENDED;
  }

  /**
   * Reactivates an archived or suspended project.
   */
  public void reactivate() {
    ensureNotDeleted();
    if (status == ProjectStatus.ACTIVE) {
      throw new DomainValidationException("project_already_active");
    }
    this.status = ProjectStatus.ACTIVE;
  }

  /**
   * Marks the project as deleted.
   */
  public void markDeleted() {
    if (status == ProjectStatus.DELETED) {
      throw new DomainValidationException("project_already_deleted");
    }
    this.status = ProjectStatus.DELETED;
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
}
