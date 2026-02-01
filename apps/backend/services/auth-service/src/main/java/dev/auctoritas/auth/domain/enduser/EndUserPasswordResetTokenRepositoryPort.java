package dev.auctoritas.auth.domain.enduser;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for EndUserPasswordResetToken persistence operations.
 */
public interface EndUserPasswordResetTokenRepositoryPort {

  Optional<EndUserPasswordResetToken> findByTokenHash(String tokenHash);

  int markUsedByUserIdAndProjectId(UUID userId, UUID projectId, Instant usedAt);

  EndUserPasswordResetToken save(EndUserPasswordResetToken token);
}
