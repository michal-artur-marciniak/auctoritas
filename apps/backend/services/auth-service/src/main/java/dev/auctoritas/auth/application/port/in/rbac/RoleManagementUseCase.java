package dev.auctoritas.auth.application.port.in.rbac;

import dev.auctoritas.auth.adapter.in.web.RoleCreateRequest;
import dev.auctoritas.auth.adapter.in.web.RolePermissionUpdateRequest;
import dev.auctoritas.auth.adapter.in.web.RoleSummaryResponse;
import dev.auctoritas.auth.adapter.in.web.RoleUpdateRequest;
import dev.auctoritas.auth.application.port.in.ApplicationPrincipal;
import java.util.UUID;

/**
 * Use case for role management operations.
 */
public interface RoleManagementUseCase {
  RoleSummaryResponse createRole(
      UUID orgId,
      UUID projectId,
      ApplicationPrincipal principal,
      RoleCreateRequest request);

  RoleSummaryResponse updateRole(
      UUID orgId,
      UUID projectId,
      UUID roleId,
      ApplicationPrincipal principal,
      RoleUpdateRequest request);

  RoleSummaryResponse updateRolePermissions(
      UUID orgId,
      UUID projectId,
      UUID roleId,
      ApplicationPrincipal principal,
      RolePermissionUpdateRequest request);

  void deleteRole(UUID orgId, UUID projectId, UUID roleId, ApplicationPrincipal principal);
}
