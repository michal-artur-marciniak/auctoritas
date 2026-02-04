package dev.auctoritas.auth.domain.rbac;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.shared.DomainEvent;
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
 * Aggregate root representing a project-scoped role.
 */
@Entity
@Table(
    name = "roles",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "name"}))
@Getter
public class Role extends BaseAuditEntity {
  private static final int DESCRIPTION_MAX_LENGTH = 255;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(length = 255)
  private String description;

  @Column(name = "is_system", nullable = false)
  private boolean system;

  @Transient
  private final List<DomainEvent> domainEvents = new ArrayList<>();

  protected Role() {
  }

  /**
   * Factory method to create a new Role.
   */
  public static Role create(Project project, RoleName name, String description, boolean isSystem) {
    if (project == null) {
      throw new DomainValidationException("project_required");
    }
    if (name == null) {
      throw new DomainValidationException("role_name_required");
    }

    Role role = new Role();
    role.project = project;
    role.name = name.value();
    role.description = normalizeDescription(description);
    role.system = isSystem;

    role.registerEvent(new RoleCreatedEvent(
        UUID.randomUUID(),
        role.getId(),
        project.getId(),
        role.name,
        role.system,
        Instant.now()
    ));

    return role;
  }

  /**
   * Renames the role.
   */
  public void rename(RoleName newName) {
    ensureMutable();
    if (newName == null) {
      throw new DomainValidationException("role_name_required");
    }
    this.name = newName.value();
    registerEvent(new RoleUpdatedEvent(
        UUID.randomUUID(),
        getId(),
        this.name,
        this.description,
        Instant.now()
    ));
  }

  /**
   * Updates the role description.
   */
  public void updateDescription(String newDescription) {
    ensureMutable();
    this.description = normalizeDescription(newDescription);
    registerEvent(new RoleUpdatedEvent(
        UUID.randomUUID(),
        getId(),
        this.name,
        this.description,
        Instant.now()
    ));
  }

  private void ensureMutable() {
    if (system) {
      throw new DomainValidationException("system_role_immutable");
    }
  }

  private static String normalizeDescription(String description) {
    if (description == null) {
      return null;
    }
    String trimmed = description.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.length() > DESCRIPTION_MAX_LENGTH) {
      throw new DomainValidationException("role_description_too_long");
    }
    return trimmed;
  }

  protected void registerEvent(DomainEvent event) {
    domainEvents.add(event);
  }

  public boolean isSystem() {
    return system;
  }

  public List<DomainEvent> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
  }

  public void clearDomainEvents() {
    domainEvents.clear();
  }
}
