package dev.auctoritas.auth.application.rbac;

import dev.auctoritas.auth.domain.rbac.Role;
import dev.auctoritas.auth.domain.rbac.RolePermission;
import dev.auctoritas.auth.domain.rbac.RolePermissionRepositoryPort;
import dev.auctoritas.auth.domain.rbac.UserRoleAssignment;
import dev.auctoritas.auth.domain.rbac.UserRoleRepositoryPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EndUserPermissionResolver {
  private final UserRoleRepositoryPort userRoleRepository;
  private final RolePermissionRepositoryPort rolePermissionRepository;

  public EndUserPermissionResolver(
      UserRoleRepositoryPort userRoleRepository,
      RolePermissionRepositoryPort rolePermissionRepository) {
    this.userRoleRepository = userRoleRepository;
    this.rolePermissionRepository = rolePermissionRepository;
  }

  @Transactional(readOnly = true)
  public ResolvedPermissions resolvePermissions(UUID userId) {
    if (userId == null) {
      return ResolvedPermissions.empty();
    }
    List<UserRoleAssignment> assignments = userRoleRepository.listByUserId(userId);
    if (assignments == null || assignments.isEmpty()) {
      return ResolvedPermissions.empty();
    }

    Set<String> roles = new LinkedHashSet<>();
    Set<String> permissions = new LinkedHashSet<>();

    for (UserRoleAssignment assignment : assignments) {
      if (assignment == null) {
        continue;
      }
      Role role = assignment.getRole();
      if (role == null || role.getId() == null) {
        continue;
      }
      String roleName = role.getName();
      if (roleName != null && !roleName.isBlank()) {
        roles.add(roleName);
      }
      List<RolePermission> rolePermissions = rolePermissionRepository.listByRoleId(role.getId());
      for (RolePermission mapping : rolePermissions) {
        if (mapping == null || mapping.getPermission() == null) {
          continue;
        }
        String code = mapping.getPermission().getCode();
        if (code != null && !code.isBlank()) {
          permissions.add(code);
        }
      }
    }

    return new ResolvedPermissions(sort(roles), sort(permissions));
  }

  private List<String> sort(Set<String> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    ArrayList<String> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    return List.copyOf(sorted);
  }

  public record ResolvedPermissions(List<String> roles, List<String> permissions) {
    public static ResolvedPermissions empty() {
      return new ResolvedPermissions(List.of(), List.of());
    }
  }
}
