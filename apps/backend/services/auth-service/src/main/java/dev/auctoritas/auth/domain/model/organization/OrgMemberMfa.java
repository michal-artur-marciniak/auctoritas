package dev.auctoritas.auth.domain.model.organization;

import dev.auctoritas.auth.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "org_member_mfa")
@Getter
@Setter
@NoArgsConstructor
public class OrgMemberMfa extends BaseEntity {
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false, unique = true)
  private OrganizationMember member;

  @Column(nullable = false, length = 255)
  private String secret;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "recovery_codes", columnDefinition = "text[]")
  private List<String> recoveryCodes;

  @Column(nullable = false)
  private Boolean enabled = false;
}
