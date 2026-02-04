package dev.auctoritas.auth.domain.rbac;

import java.util.List;
import java.util.UUID;

/**
 * Port for UserRoleAssignment persistence operations.
 */
public interface UserRoleRepositoryPort {
  List<UserRoleAssignment> listByUserId(UUID userId);

  List<UserRoleAssignment> replaceAssignments(UUID userId, List<Role> roles);

  void deleteByUserId(UUID userId);
}
