package dev.auctoritas.auth.entity.organization;

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
    name = "org_member_refresh_tokens",
    uniqueConstraints = @UniqueConstraint(columnNames = {"token_hash"}))
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private OrganizationMember member;

  @Column(name = "token_hash", nullable = false, length = 255)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private Boolean revoked = false;

  @Column(name = "replaced_by", length = 255)
  private String replacedBy;

  @Column(name = "user_agent", columnDefinition = "TEXT")
  private String userAgent;

  @Column(name = "ip_address")
  private String ipAddress;
}
