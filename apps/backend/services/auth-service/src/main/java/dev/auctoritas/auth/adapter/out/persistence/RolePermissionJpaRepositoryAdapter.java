package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.adapter.out.persistence.repository.RolePermissionRepository;
import dev.auctoritas.auth.domain.rbac.Permission;
import dev.auctoritas.auth.domain.rbac.RolePermission;
import dev.auctoritas.auth.domain.rbac.RolePermissionRepositoryPort;
import dev.auctoritas.auth.domain.rbac.RoleRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter exposing {@link RolePermissionRepository} via {@link RolePermissionRepositoryPort}.
 */
@Component
public class RolePermissionJpaRepositoryAdapter implements RolePermissionRepositoryPort {
  private final RolePermissionRepository rolePermissionRepository;
  private final RoleRepositoryPort roleRepositoryPort;

  public RolePermissionJpaRepositoryAdapter(
      RolePermissionRepository rolePermissionRepository,
      RoleRepositoryPort roleRepositoryPort) {
    this.rolePermissionRepository = rolePermissionRepository;
    this.roleRepositoryPort = roleRepositoryPort;
  }

  @Override
  @Transactional
  public void setPermissions(UUID roleId, List<Permission> permissions) {
    rolePermissionRepository.deleteByRoleId(roleId);
    if (permissions == null || permissions.isEmpty()) {
      return;
    }
    var role = roleRepositoryPort.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("role_not_found"));
    List<RolePermission> mappings = permissions.stream()
        .map(permission -> RolePermission.create(role, permission))
        .toList();
    rolePermissionRepository.saveAll(mappings);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RolePermission> listByRoleId(UUID roleId) {
    return rolePermissionRepository.findByRoleId(roleId);
  }
}
