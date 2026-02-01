package dev.auctoritas.auth.adapter.out.persistence;

import dev.auctoritas.auth.adapter.out.persistence.repository.MfaChallengeRepository;
import dev.auctoritas.auth.domain.mfa.MfaChallenge;
import dev.auctoritas.auth.domain.mfa.MfaChallengeRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter exposing {@link MfaChallengeRepository} via {@link MfaChallengeRepositoryPort}.
 * Implements the outbound persistence adapter for MFA challenges following Hexagonal Architecture.
 */
@Component
public class MfaChallengeJpaRepositoryAdapter implements MfaChallengeRepositoryPort {

  private final MfaChallengeRepository mfaChallengeRepository;

  public MfaChallengeJpaRepositoryAdapter(MfaChallengeRepository mfaChallengeRepository) {
    this.mfaChallengeRepository = mfaChallengeRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MfaChallenge> findByToken(String token) {
    return mfaChallengeRepository.findByToken(token);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MfaChallenge> findByTokenForUpdate(String token) {
    return mfaChallengeRepository.findByTokenForUpdate(token);
  }

  @Override
  @Transactional
  public MfaChallenge save(MfaChallenge challenge) {
    return mfaChallengeRepository.save(challenge);
  }

  @Override
  @Transactional
  public void delete(MfaChallenge challenge) {
    mfaChallengeRepository.delete(challenge);
  }

  @Override
  @Transactional
  public int deleteExpired(Instant now) {
    return mfaChallengeRepository.deleteByExpiresAtBefore(now);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MfaChallenge> findByUserId(UUID userId) {
    return mfaChallengeRepository.findByUserId(userId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MfaChallenge> findByMemberId(UUID memberId) {
    return mfaChallengeRepository.findByMemberId(memberId);
  }

  @Override
  @Transactional
  public void markAsUsed(UUID id) {
    mfaChallengeRepository.markAsUsed(id);
  }
}
