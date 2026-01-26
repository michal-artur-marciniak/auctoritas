package dev.auctoritas.auth.entity.enduser;

import dev.auctoritas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "end_user_email_verification_tokens")
@Getter
@Setter
@NoArgsConstructor
public class EndUserEmailVerificationToken extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private EndUser user;

  @Column(name = "token_hash", nullable = false, length = 128)
  private String tokenHash;

  @Column(name = "code_hash", nullable = false, length = 128)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;
}
