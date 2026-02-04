package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.adapter.out.persistence.repository.PermissionRepository;
import dev.auctoritas.auth.domain.rbac.Permission;
import dev.auctoritas.auth.domain.rbac.PermissionRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter exposing {@link PermissionRepository} via {@link PermissionRepositoryPort}.
 */
@Component
public class PermissionJpaRepositoryAdapter implements PermissionRepositoryPort {
  private final PermissionRepository permissionRepository;

  public PermissionJpaRepositoryAdapter(PermissionRepository permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Permission> findByCode(String code) {
    return permissionRepository.findByCode(code);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Permission> listAll() {
    return permissionRepository.findAll();
  }

  @Override
  @Transactional
  public List<Permission> saveAll(List<Permission> permissions) {
    return permissionRepository.saveAll(permissions);
  }
}
