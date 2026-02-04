package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.adapter.out.persistence.repository.UserRoleRepository;
import dev.auctoritas.auth.domain.enduser.EndUserRepositoryPort;
import dev.auctoritas.auth.domain.rbac.Role;
import dev.auctoritas.auth.domain.rbac.UserRoleAssignment;
import dev.auctoritas.auth.domain.rbac.UserRoleRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter exposing {@link UserRoleRepository} via {@link UserRoleRepositoryPort}.
 */
@Component
public class UserRoleJpaRepositoryAdapter implements UserRoleRepositoryPort {
  private final UserRoleRepository userRoleRepository;
  private final EndUserRepositoryPort endUserRepositoryPort;

  public UserRoleJpaRepositoryAdapter(
      UserRoleRepository userRoleRepository,
      EndUserRepositoryPort endUserRepositoryPort) {
    this.userRoleRepository = userRoleRepository;
    this.endUserRepositoryPort = endUserRepositoryPort;
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserRoleAssignment> listByUserId(UUID userId) {
    return userRoleRepository.findByUserId(userId);
  }

  @Override
  @Transactional
  public List<UserRoleAssignment> replaceAssignments(UUID userId, List<Role> roles) {
    userRoleRepository.deleteByUserId(userId);
    if (roles == null || roles.isEmpty()) {
      return List.of();
    }
    Role firstRole = roles.get(0);
    if (firstRole == null || firstRole.getProject() == null) {
      throw new IllegalArgumentException("role_project_required");
    }
    var projectId = firstRole.getProject().getId();
    var user = endUserRepositoryPort.findByIdAndProjectIdForUpdate(userId, projectId)
        .orElseThrow(() -> new IllegalArgumentException("user_not_found"));
    List<UserRoleAssignment> assignments = roles.stream()
        .map(role -> UserRoleAssignment.assign(user, role))
        .toList();
    return userRoleRepository.saveAll(assignments);
  }

  @Override
  @Transactional
  public void deleteByUserId(UUID userId) {
    userRoleRepository.deleteByUserId(userId);
  }
}
