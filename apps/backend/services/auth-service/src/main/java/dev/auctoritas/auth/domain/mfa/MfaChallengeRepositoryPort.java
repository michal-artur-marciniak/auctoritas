package dev.auctoritas.auth.domain.mfa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for MfaChallenge persistence operations.
 * Repository port in the domain layer following Hexagonal Architecture.
 */
public interface MfaChallengeRepositoryPort {

  /**
   * Find a challenge by its token.
   *
   * @param token the challenge token
   * @return optional containing the challenge if found
   */
  Optional<MfaChallenge> findByToken(String token);

  /**
   * Find a challenge by its token with pessimistic lock.
   *
   * @param token the challenge token
   * @return optional containing the challenge if found
   */
  Optional<MfaChallenge> findByTokenForUpdate(String token);

  /**
   * Save a challenge.
   *
   * @param challenge the challenge to save
   * @return the saved challenge
   */
  MfaChallenge save(MfaChallenge challenge);

  /**
   * Delete a challenge.
   *
   * @param challenge the challenge to delete
   */
  void delete(MfaChallenge challenge);

  /**
   * Delete all expired challenges.
   *
   * @param now the current time
   * @return number of deleted challenges
   */
  int deleteExpired(Instant now);

  /**
   * Find all challenges for a user.
   *
   * @param userId the user ID
   * @return list of challenges
   */
  List<MfaChallenge> findByUserId(UUID userId);

  /**
   * Find all challenges for an organization member.
   *
   * @param memberId the member ID
   * @return list of challenges
   */
  List<MfaChallenge> findByMemberId(UUID memberId);

  /**
   * Mark a challenge as used.
   *
   * @param id the challenge ID
   */
  void markAsUsed(UUID id);
}
