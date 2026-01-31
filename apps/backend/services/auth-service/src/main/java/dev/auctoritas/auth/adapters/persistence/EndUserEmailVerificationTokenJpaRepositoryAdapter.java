package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.entity.enduser.EndUserEmailVerificationToken;
import dev.auctoritas.auth.ports.identity.EndUserEmailVerificationTokenRepositoryPort;
import dev.auctoritas.auth.repository.EndUserEmailVerificationTokenRepository;
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
