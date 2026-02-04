package dev.auctoritas.auth.domain.rbac;

import dev.auctoritas.auth.domain.enduser.EndUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * Assignment of project roles to an end user.
 */
@Entity
@Table(name = "user_roles")
@IdClass(UserRoleAssignmentId.class)
@Getter
public class UserRoleAssignment {
  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Id
  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
  private EndUser user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id", nullable = false, insertable = false, updatable = false)
  private Role role;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected UserRoleAssignment() {
  }

  /**
   * Assigns a role to a user.
   */
  public static UserRoleAssignment assign(EndUser user, Role role) {
    if (user == null || role == null) {
      throw new IllegalArgumentException("user_and_role_required");
    }
    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.userId = user.getId();
    assignment.roleId = role.getId();
    assignment.user = user;
    assignment.role = role;
    assignment.createdAt = Instant.now();
    return assignment;
  }

  public UUID getUserId() {
    return user != null ? user.getId() : null;
  }

  public UUID getRoleId() {
    return role != null ? role.getId() : null;
  }
}
