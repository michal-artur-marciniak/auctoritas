package dev.auctoritas.auth.domain.rbac;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite identifier for RolePermission.
 */
public class RolePermissionId implements Serializable {
  private UUID roleId;
  private UUID permissionId;

  public RolePermissionId() {
  }

  public RolePermissionId(UUID roleId, UUID permissionId) {
    this.roleId = roleId;
    this.permissionId = permissionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RolePermissionId that = (RolePermissionId) o;
    return Objects.equals(roleId, that.roleId)
        && Objects.equals(permissionId, that.permissionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId, permissionId);
  }
}
