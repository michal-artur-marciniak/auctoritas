package dev.auctoritas.auth.entity.oauth;

import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "oauth_connections",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"project_id", "provider", "provider_user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class OAuthConnection extends BaseAuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private EndUser user;

  @Column(nullable = false, length = 50)
  private String provider;

  @Column(name = "provider_user_id", nullable = false, length = 255)
  private String providerUserId;

  @Column(nullable = false, length = 255)
  private String email;
}
