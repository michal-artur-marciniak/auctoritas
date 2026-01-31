package dev.auctoritas.auth.domain.model.enduser;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for EndUserRefreshToken persistence operations.
 */
public interface EndUserRefreshTokenRepositoryPort {

  Optional<EndUserRefreshToken> findByTokenHash(String tokenHash);

  Optional<EndUserRefreshToken> findTopByUserIdAndRevokedFalseOrderByCreatedAtDesc(UUID userId);

  int revokeActiveByUserId(UUID userId);

  int revokeActiveByUserIdExcludingId(UUID userId, UUID excludedId);

  EndUserRefreshToken save(EndUserRefreshToken token);
}
