package dev.auctoritas.auth.infrastructure.persistence;

import dev.auctoritas.auth.domain.enduser.EndUserEmailVerificationToken;
import dev.auctoritas.auth.domain.enduser.EndUserEmailVerificationTokenRepositoryPort;
import dev.auctoritas.auth.infrastructure.persistence.repository.EndUserEmailVerificationTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link EndUserEmailVerificationTokenRepository} via {@link EndUserEmailVerificationTokenRepositoryPort}.
 */
@Component
public class EndUserEmailVerificationTokenJpaRepositoryAdapter implements EndUserEmailVerificationTokenRepositoryPort {

  private final EndUserEmailVerificationTokenRepository endUserEmailVerificationTokenRepository;

  public EndUserEmailVerificationTokenJpaRepositoryAdapter(EndUserEmailVerificationTokenRepository endUserEmailVerificationTokenRepository) {
    this.endUserEmailVerificationTokenRepository = endUserEmailVerificationTokenRepository;
  }

  @Override
  public Optional<EndUserEmailVerificationToken> findByTokenHash(String tokenHash) {
    return endUserEmailVerificationTokenRepository.findByTokenHash(tokenHash);
  }

  @Override
  public long countIssuedSince(UUID userId, UUID projectId, Instant since) {
    return endUserEmailVerificationTokenRepository.countIssuedSince(userId, projectId, since);
  }

  @Override
  public int markUsedByUserId(UUID userId, Instant usedAt) {
    return endUserEmailVerificationTokenRepository.markUsedByUserId(userId, usedAt);
  }

  @Override
  public EndUserEmailVerificationToken save(EndUserEmailVerificationToken token) {
    return endUserEmailVerificationTokenRepository.save(token);
  }
}
