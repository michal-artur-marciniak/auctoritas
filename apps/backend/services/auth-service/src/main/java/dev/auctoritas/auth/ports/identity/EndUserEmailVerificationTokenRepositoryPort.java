package dev.auctoritas.auth.ports.identity;

import dev.auctoritas.auth.entity.enduser.EndUserEmailVerificationToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for EndUserEmailVerificationToken persistence operations.
 */
public interface EndUserEmailVerificationTokenRepositoryPort {

  Optional<EndUserEmailVerificationToken> findByTokenHash(String tokenHash);

  long countIssuedSince(UUID userId, UUID projectId, Instant since);

  int markUsedByUserId(UUID userId, Instant usedAt);

  EndUserEmailVerificationToken save(EndUserEmailVerificationToken token);
}
