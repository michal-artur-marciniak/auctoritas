package dev.auctoritas.auth.domain.rbac;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite identifier for UserRoleAssignment.
 */
public class UserRoleAssignmentId implements Serializable {
  private UUID userId;
  private UUID roleId;

  public UserRoleAssignmentId() {
  }

  public UserRoleAssignmentId(UUID userId, UUID roleId) {
    this.userId = userId;
    this.roleId = roleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserRoleAssignmentId that = (UserRoleAssignmentId) o;
    return Objects.equals(userId, that.userId)
        && Objects.equals(roleId, that.roleId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roleId);
  }
}
