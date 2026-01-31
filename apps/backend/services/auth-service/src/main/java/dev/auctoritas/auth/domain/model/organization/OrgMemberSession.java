package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "org_member_sessions")
@Getter
@Setter
@NoArgsConstructor
public class OrgMemberSession extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private OrganizationMember member;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "device_info", columnDefinition = "jsonb")
  private Map<String, Object> deviceInfo;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
}
