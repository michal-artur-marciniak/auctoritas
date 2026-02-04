package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.adapter.out.persistence.repository.RoleRepository;
import dev.auctoritas.auth.domain.rbac.Role;
import dev.auctoritas.auth.domain.rbac.RoleRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter exposing {@link RoleRepository} via {@link RoleRepositoryPort}.
 */
@Component
public class RoleJpaRepositoryAdapter implements RoleRepositoryPort {
  private final RoleRepository roleRepository;

  public RoleJpaRepositoryAdapter(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Role> findById(UUID roleId) {
    return roleRepository.findById(roleId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Role> findByNameAndProjectId(String name, UUID projectId) {
    return roleRepository.findByNameAndProjectId(name, projectId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Role> listByProjectId(UUID projectId) {
    return roleRepository.findAllByProjectId(projectId);
  }

  @Override
  @Transactional
  public Role save(Role role) {
    return roleRepository.save(role);
  }

  @Override
  @Transactional
  public void delete(Role role) {
    roleRepository.delete(role);
  }
}
