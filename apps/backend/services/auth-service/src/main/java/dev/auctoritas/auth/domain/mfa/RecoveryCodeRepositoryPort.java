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
  List<MfaRecoveryCode> findByUserId(UUID userId);

  /**
   * Find all recovery codes for an organization member.
   *
   * @param memberId the member ID
   * @return list of recovery codes
   */
  List<MfaRecoveryCode> findByMemberId(UUID memberId);

  /**
   * Find a recovery code by its hash.
   *
   * @param codeHash the SHA-256 hash of the code
   * @return optional containing the recovery code if found
   */
  Optional<MfaRecoveryCode> findByCodeHash(String codeHash);

  /**
   * Save all recovery codes.
   *
   * @param codes the recovery codes to save
   * @return the saved recovery codes
   */
  List<MfaRecoveryCode> saveAll(List<MfaRecoveryCode> codes);

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
}
