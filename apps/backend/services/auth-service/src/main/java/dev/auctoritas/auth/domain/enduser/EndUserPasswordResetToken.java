package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class EndUserPasswordResetToken extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private EndUser user;

  @Column(name = "token_hash", nullable = false, length = 128)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", length = 500)
  private String userAgent;

  public static EndUserPasswordResetToken issue(
      Project project,
      EndUser user,
      String tokenHash,
      Instant expiresAt,
      String ipAddress,
      String userAgent) {
    Objects.requireNonNull(project, "project_required");
    Objects.requireNonNull(user, "user_required");
    if (tokenHash == null || tokenHash.isBlank()) {
      throw new DomainValidationException("reset_token_required");
    }
    Objects.requireNonNull(expiresAt, "expires_at_required");

    EndUserPasswordResetToken token = new EndUserPasswordResetToken();
    token.project = project;
    token.user = user;
    token.tokenHash = tokenHash;
    token.expiresAt = expiresAt;
    token.ipAddress = ipAddress;
    token.userAgent = userAgent;
    return token;
  }

  public boolean belongsToProject(UUID projectId) {
    if (projectId == null) {
      return false;
    }
    if (project == null || project.getId() == null) {
      return false;
    }
    return projectId.equals(project.getId());
  }

  public boolean isExpired(Instant now) {
    if (now == null) {
      return false;
    }
    return expiresAt != null && expiresAt.isBefore(now);
  }

  public boolean isUsed() {
    return usedAt != null;
  }

  public void markUsed(Instant now) {
    if (now == null) {
      throw new DomainValidationException("reset_token_used_at_required");
    }
    this.usedAt = now;
  }
}
