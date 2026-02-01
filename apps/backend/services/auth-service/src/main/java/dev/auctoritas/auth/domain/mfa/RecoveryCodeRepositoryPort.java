package dev.auctoritas.auth.domain.mfa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for RecoveryCode persistence operations.
 * Repository port in the domain layer following Hexagonal Architecture.
 */
public interface RecoveryCodeRepositoryPort {

  /**
   * Find all recovery codes for a user.
   *
   * @param userId the user ID
   * @return list of recovery codes
   */
  List<RecoveryCodeEntity> findByUserId(UUID userId);

  /**
   * Find all recovery codes for an organization member.
   *
   * @param memberId the member ID
   * @return list of recovery codes
   */
  List<RecoveryCodeEntity> findByMemberId(UUID memberId);

  /**
   * Find a recovery code by its hash.
   *
   * @param codeHash the SHA-256 hash of the code
   * @return optional containing the recovery code if found
   */
  Optional<RecoveryCodeEntity> findByCodeHash(String codeHash);

  /**
   * Save all recovery codes.
   *
   * @param codes the recovery codes to save
   * @return the saved recovery codes
   */
  List<RecoveryCodeEntity> saveAll(List<RecoveryCodeEntity> codes);

  /**
   * Delete all recovery codes for a user.
   *
   * @param userId the user ID
   */
  void deleteByUserId(UUID userId);

  /**
   * Delete all recovery codes for a member.
   *
   * @param memberId the member ID
   */
  void deleteByMemberId(UUID memberId);

  /**
   * Mark a recovery code as used.
   *
   * @param id the recovery code ID
   */
  void markAsUsed(UUID id);

  /**
   * Entity class for recovery code persistence.
   * Separate from domain RecoveryCode value object to handle JPA requirements.
   */
  class RecoveryCodeEntity {
    private UUID id;
    private UUID userId;
    private UUID memberId;
    private String codeHash;
    private boolean used;
    private java.time.Instant usedAt;
    private java.time.Instant createdAt;

    public RecoveryCodeEntity() {}

    public RecoveryCodeEntity(UUID id, UUID userId, UUID memberId, String codeHash, boolean used,
        java.time.Instant usedAt, java.time.Instant createdAt) {
      this.id = id;
      this.userId = userId;
      this.memberId = memberId;
      this.codeHash = codeHash;
      this.used = used;
      this.usedAt = usedAt;
      this.createdAt = createdAt;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public java.time.Instant getUsedAt() { return usedAt; }
    public void setUsedAt(java.time.Instant usedAt) { this.usedAt = usedAt; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
  }
}
