package dev.auctoritas.auth.entity.enduser;

import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "end_users",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "email"}))
@Getter
@Setter
@NoArgsConstructor
public class EndUser extends BaseAuditEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(length = 100)
  private String name;

  @Column(name = "email_verified", nullable = false)
  private Boolean emailVerified = false;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts = 0;

  @Column(name = "failed_login_window_start")
  private Instant failedLoginWindowStart;

  @Column(name = "lockout_until")
  private Instant lockoutUntil;
}
