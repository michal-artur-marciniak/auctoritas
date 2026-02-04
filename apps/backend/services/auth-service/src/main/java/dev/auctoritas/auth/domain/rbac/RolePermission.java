package dev.auctoritas.auth.domain.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * Entity representing the role to permission mapping.
 */
@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionId.class)
@Getter
public class RolePermission {
  @Id
  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  @Id
  @Column(name = "permission_id", nullable = false)
  private UUID permissionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id", nullable = false, insertable = false, updatable = false)
  private Role role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "permission_id", nullable = false, insertable = false, updatable = false)
  private Permission permission;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected RolePermission() {
  }

  public static RolePermission create(Role role, Permission permission) {
    if (role == null || permission == null) {
      throw new IllegalArgumentException("role_and_permission_required");
    }
    RolePermission mapping = new RolePermission();
    mapping.roleId = role.getId();
    mapping.permissionId = permission.getId();
    mapping.role = role;
    mapping.permission = permission;
    mapping.createdAt = Instant.now();
    return mapping;
  }
}
