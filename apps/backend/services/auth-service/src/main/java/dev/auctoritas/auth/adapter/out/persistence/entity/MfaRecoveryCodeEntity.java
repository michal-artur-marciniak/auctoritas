package dev.auctoritas.auth.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for MFA recovery codes.
 * Separate from domain model to handle persistence concerns.
 */
@Entity
@Table(name = "mfa_recovery_codes")
public class MfaRecoveryCodeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "member_id")
  private UUID memberId;

  @Column(name = "code_hash", nullable = false, length = 255)
  private String codeHash;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public MfaRecoveryCodeEntity() {
    this.createdAt = Instant.now();
  }

  public MfaRecoveryCodeEntity(UUID id, UUID userId, UUID memberId, String codeHash,
      Instant usedAt, Instant createdAt) {
    this.id = id;
    this.userId = userId;
    this.memberId = memberId;
    this.codeHash = codeHash;
    this.usedAt = usedAt;
    this.createdAt = createdAt;
  }

  // Factory methods
  public static MfaRecoveryCodeEntity forUser(UUID userId, String codeHash) {
    MfaRecoveryCodeEntity entity = new MfaRecoveryCodeEntity();
    entity.userId = userId;
    entity.codeHash = codeHash;
    return entity;
  }

  public static MfaRecoveryCodeEntity forMember(UUID memberId, String codeHash) {
    MfaRecoveryCodeEntity entity = new MfaRecoveryCodeEntity();
    entity.memberId = memberId;
    entity.codeHash = codeHash;
    return entity;
  }

  // Getters and setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getMemberId() {
    return memberId;
  }

  public void setMemberId(UUID memberId) {
    this.memberId = memberId;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public void setCodeHash(String codeHash) {
    this.codeHash = codeHash;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(Instant usedAt) {
    this.usedAt = usedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public boolean isUsed() {
    return usedAt != null;
  }

  public void markUsed() {
    this.usedAt = Instant.now();
  }
}
