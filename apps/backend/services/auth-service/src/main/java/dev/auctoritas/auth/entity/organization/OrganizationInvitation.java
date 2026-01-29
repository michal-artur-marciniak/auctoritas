package dev.auctoritas.auth.entity.organization;

import dev.auctoritas.auth.shared.persistence.BaseEntity;
import dev.auctoritas.auth.shared.enums.OrgMemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organization_invitations")
@Getter
@Setter
@NoArgsConstructor
public class OrganizationInvitation extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 255)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrgMemberRole role;

  @Column(nullable = false, unique = true, length = 255)
  private String token;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invited_by")
  private OrganizationMember invitedBy;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
}
