package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * Entity representing an MFA recovery code.
 * Stores SHA-256 hash of the code (not the plain code) and tracks usage.
 */
@Entity
@Table(name = "mfa_recovery_codes")
@Getter
public class MfaRecoveryCode extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private EndUser user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id")
  private OrganizationMember member;

  @Column(name = "code_hash", nullable = false, length = 255)
  private String codeHash;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected MfaRecoveryCode() {
  }

  /**
   * Creates a recovery code for an end user.
   *
   * @param user the end user
   * @param codeHash the SHA-256 hash of the recovery code
   * @return new MfaRecoveryCode
   */
  public static MfaRecoveryCode createForUser(EndUser user, String codeHash) {
    if (user == null) {
      throw new IllegalArgumentException("User is required");
    }
    if (codeHash == null || codeHash.isEmpty()) {
      throw new IllegalArgumentException("Code hash is required");
    }

    MfaRecoveryCode code = new MfaRecoveryCode();
    code.user = user;
    code.codeHash = codeHash;
    code.createdAt = Instant.now();
    return code;
  }

  /**
   * Creates a recovery code for an organization member.
   *
   * @param member the organization member
   * @param codeHash the SHA-256 hash of the recovery code
   * @return new MfaRecoveryCode
   */
  public static MfaRecoveryCode createForMember(OrganizationMember member, String codeHash) {
    if (member == null) {
      throw new IllegalArgumentException("Member is required");
    }
    if (codeHash == null || codeHash.isEmpty()) {
      throw new IllegalArgumentException("Code hash is required");
    }

    MfaRecoveryCode code = new MfaRecoveryCode();
    code.member = member;
    code.codeHash = codeHash;
    code.createdAt = Instant.now();
    return code;
  }

  /**
   * Checks if this recovery code has been used.
   */
  public boolean isUsed() {
    return usedAt != null;
  }

  /**
   * Marks this recovery code as used.
   */
  public void markUsed() {
    if (isUsed()) {
      throw new IllegalStateException("Recovery code already used");
    }
    this.usedAt = Instant.now();
  }

  /**
   * Returns the user ID if this code belongs to a user.
   */
  public UUID getUserId() {
    return user != null ? user.getId() : null;
  }

  /**
   * Returns the member ID if this code belongs to a member.
   */
  public UUID getMemberId() {
    return member != null ? member.getId() : null;
  }
}
