package dev.auctoritas.auth.domain.rbac;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for Role persistence operations.
 */
public interface RoleRepositoryPort {
  Optional<Role> findById(UUID roleId);

  Optional<Role> findByNameAndProjectId(String name, UUID projectId);

  List<Role> listByProjectId(UUID projectId);

  Role save(Role role);

  void delete(Role role);
}
