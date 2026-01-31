package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.domain.model.enduser.EndUserPasswordResetToken;
import dev.auctoritas.auth.domain.model.enduser.EndUserPasswordResetTokenRepositoryPort;
import dev.auctoritas.auth.repository.EndUserPasswordResetTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link EndUserPasswordResetTokenRepository} via {@link EndUserPasswordResetTokenRepositoryPort}.
 */
@Component
public class EndUserPasswordResetTokenJpaRepositoryAdapter implements EndUserPasswordResetTokenRepositoryPort {

  private final EndUserPasswordResetTokenRepository endUserPasswordResetTokenRepository;

  public EndUserPasswordResetTokenJpaRepositoryAdapter(EndUserPasswordResetTokenRepository endUserPasswordResetTokenRepository) {
    this.endUserPasswordResetTokenRepository = endUserPasswordResetTokenRepository;
  }

  @Override
  public Optional<EndUserPasswordResetToken> findByTokenHash(String tokenHash) {
    return endUserPasswordResetTokenRepository.findByTokenHash(tokenHash);
  }

  @Override
  public int markUsedByUserIdAndProjectId(UUID userId, UUID projectId, Instant usedAt) {
    return endUserPasswordResetTokenRepository.markUsedByUserIdAndProjectId(userId, projectId, usedAt);
  }

  @Override
  public EndUserPasswordResetToken save(EndUserPasswordResetToken token) {
    return endUserPasswordResetTokenRepository.save(token);
  }
}
