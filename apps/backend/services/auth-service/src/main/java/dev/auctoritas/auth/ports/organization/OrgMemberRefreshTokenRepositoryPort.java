package dev.auctoritas.auth.ports.organization;

import dev.auctoritas.auth.domain.model.organization.OrgMemberRefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for OrgMemberRefreshToken persistence operations.
 */
public interface OrgMemberRefreshTokenRepositoryPort {

  Optional<OrgMemberRefreshToken> findByTokenHash(String tokenHash);

  void deleteExpiredTokens(Instant now);

  void revokeAllByMemberId(UUID memberId);

  OrgMemberRefreshToken save(OrgMemberRefreshToken token);
}
