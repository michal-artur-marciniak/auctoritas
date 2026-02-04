package dev.auctoritas.auth.application.port.in.rbac;

import dev.auctoritas.auth.adapter.in.web.UserRoleAssignmentRequest;
import dev.auctoritas.auth.adapter.in.web.UserRoleAssignmentResponse;
import dev.auctoritas.auth.application.port.in.ApplicationPrincipal;
import java.util.UUID;

/**
 * Use case for assigning roles to end users.
 */
public interface UserRoleAssignmentUseCase {
  UserRoleAssignmentResponse assignRoles(
      UUID orgId,
      UUID projectId,
      UUID userId,
      ApplicationPrincipal principal,
      UserRoleAssignmentRequest request);
}
