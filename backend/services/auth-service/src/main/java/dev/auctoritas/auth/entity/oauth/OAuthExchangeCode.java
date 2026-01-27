package dev.auctoritas.auth.entity.oauth;

import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.common.entity.BaseEntity;
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
    name = "oauth_exchange_codes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"code_hash"}))
@Getter
@Setter
@NoArgsConstructor
public class OAuthExchangeCode extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private EndUser user;

  @Column(nullable = false, length = 50)
  private String provider;

  @Column(name = "code_hash", nullable = false, length = 128)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;
}
