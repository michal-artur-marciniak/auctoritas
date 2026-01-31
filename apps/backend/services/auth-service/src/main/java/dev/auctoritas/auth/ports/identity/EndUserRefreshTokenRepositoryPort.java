package dev.auctoritas.auth.ports.identity;

import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
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
