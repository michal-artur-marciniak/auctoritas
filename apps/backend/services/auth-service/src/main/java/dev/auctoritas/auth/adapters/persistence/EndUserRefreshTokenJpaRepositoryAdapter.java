package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.repository.EndUserRefreshTokenRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link EndUserRefreshTokenRepository} via {@link EndUserRefreshTokenRepositoryPort}.
 */
@Component
public class EndUserRefreshTokenJpaRepositoryAdapter implements EndUserRefreshTokenRepositoryPort {

  private final EndUserRefreshTokenRepository endUserRefreshTokenRepository;

  public EndUserRefreshTokenJpaRepositoryAdapter(EndUserRefreshTokenRepository endUserRefreshTokenRepository) {
    this.endUserRefreshTokenRepository = endUserRefreshTokenRepository;
  }

  @Override
  public Optional<EndUserRefreshToken> findByTokenHash(String tokenHash) {
    return endUserRefreshTokenRepository.findByTokenHash(tokenHash);
  }

  @Override
  public Optional<EndUserRefreshToken> findTopByUserIdAndRevokedFalseOrderByCreatedAtDesc(UUID userId) {
    return endUserRefreshTokenRepository.findTopByUserIdAndRevokedFalseOrderByCreatedAtDesc(userId);
  }

  @Override
  public int revokeActiveByUserId(UUID userId) {
    return endUserRefreshTokenRepository.revokeActiveByUserId(userId);
  }

  @Override
  public int revokeActiveByUserIdExcludingId(UUID userId, UUID excludedId) {
    return endUserRefreshTokenRepository.revokeActiveByUserIdExcludingId(userId, excludedId);
  }

  @Override
  public EndUserRefreshToken save(EndUserRefreshToken token) {
    return endUserRefreshTokenRepository.save(token);
  }
}
