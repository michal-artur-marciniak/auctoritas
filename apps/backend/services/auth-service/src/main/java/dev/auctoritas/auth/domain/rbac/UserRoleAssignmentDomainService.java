package dev.auctoritas.auth.domain.rbac;

import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Domain service for validating user role assignments.
 */
@Component
public class UserRoleAssignmentDomainService {
  public void validateProjectAlignment(EndUser user, List<Role> roles) {
    Objects.requireNonNull(user, "user_required");
    if (roles == null) {
      throw new DomainValidationException("roles_required");
    }
    UUID projectId = user.getProject() != null ? user.getProject().getId() : null;
    for (Role role : roles) {
      if (role == null || role.getProject() == null || role.getProject().getId() == null) {
        throw new DomainValidationException("role_project_required");
      }
      if (projectId == null || !projectId.equals(role.getProject().getId())) {
        throw new DomainValidationException("role_project_mismatch");
      }
    }
  }
}
