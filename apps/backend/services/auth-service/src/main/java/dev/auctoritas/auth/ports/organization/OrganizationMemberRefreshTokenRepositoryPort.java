package dev.auctoritas.auth.ports.organization;

import dev.auctoritas.auth.domain.model.organization.OrganizationMemberRefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for OrganizationMemberRefreshToken persistence operations.
 */
public interface OrganizationMemberRefreshTokenRepositoryPort {

  Optional<OrganizationMemberRefreshToken> findByTokenHash(String tokenHash);

  void deleteExpiredTokens(Instant now);

  void revokeAllByMemberId(UUID memberId);

  OrganizationMemberRefreshToken save(OrganizationMemberRefreshToken token);
}
