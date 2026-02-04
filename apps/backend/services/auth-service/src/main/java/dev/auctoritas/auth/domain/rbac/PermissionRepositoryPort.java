package dev.auctoritas.auth.domain.rbac;

import java.util.List;
import java.util.Optional;

/**
 * Port for Permission persistence operations.
 */
public interface PermissionRepositoryPort {
  Optional<Permission> findByCode(String code);

  List<Permission> listAll();

  List<Permission> saveAll(List<Permission> permissions);
}
