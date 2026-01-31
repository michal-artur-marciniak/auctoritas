package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshToken;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshTokenRepositoryPort;
import dev.auctoritas.auth.repository.OrganizationMemberRefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OrganizationMemberRefreshTokenRepository} via {@link OrganizationMemberRefreshTokenRepositoryPort}.
 */
@Component
public class OrganizationMemberRefreshTokenJpaRepositoryAdapter implements OrganizationMemberRefreshTokenRepositoryPort {

  private final OrganizationMemberRefreshTokenRepository orgMemberRefreshTokenRepository;

  public OrganizationMemberRefreshTokenJpaRepositoryAdapter(OrganizationMemberRefreshTokenRepository orgMemberRefreshTokenRepository) {
    this.orgMemberRefreshTokenRepository = orgMemberRefreshTokenRepository;
  }

  @Override
  public Optional<OrganizationMemberRefreshToken> findByTokenHash(String tokenHash) {
    return orgMemberRefreshTokenRepository.findByTokenHash(tokenHash);
  }

  @Override
  public void deleteExpiredTokens(Instant now) {
    orgMemberRefreshTokenRepository.deleteExpiredTokens(now);
  }

  @Override
  public void revokeAllByMemberId(UUID memberId) {
    orgMemberRefreshTokenRepository.revokeAllByMemberId(memberId);
  }

  @Override
  public OrganizationMemberRefreshToken save(OrganizationMemberRefreshToken token) {
    return orgMemberRefreshTokenRepository.save(token);
  }
}
