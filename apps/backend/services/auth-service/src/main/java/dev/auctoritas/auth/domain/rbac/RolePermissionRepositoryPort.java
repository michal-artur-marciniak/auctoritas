package dev.auctoritas.auth.domain.rbac;

import java.util.List;
import java.util.UUID;

/**
 * Port for RolePermission persistence operations.
 */
public interface RolePermissionRepositoryPort {
  void setPermissions(UUID roleId, List<Permission> permissions);

  List<RolePermission> listByRoleId(UUID roleId);
}
