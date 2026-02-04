package dev.auctoritas.auth.application.port.in.rbac;

import dev.auctoritas.auth.adapter.in.web.RoleSummaryResponse;
import dev.auctoritas.auth.application.port.in.ApplicationPrincipal;
import java.util.List;
import java.util.UUID;

/**
 * Use case for role query operations.
 */
public interface RoleQueryUseCase {
  List<RoleSummaryResponse> listRoles(UUID orgId, UUID projectId, ApplicationPrincipal principal);
}
