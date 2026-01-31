package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.entity.organization.OrgMemberRefreshToken;
import dev.auctoritas.auth.ports.organization.OrgMemberRefreshTokenRepositoryPort;
import dev.auctoritas.auth.repository.OrgMemberRefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OrgMemberRefreshTokenRepository} via {@link OrgMemberRefreshTokenRepositoryPort}.
 */
@Component
public class OrgMemberRefreshTokenJpaRepositoryAdapter implements OrgMemberRefreshTokenRepositoryPort {

  private final OrgMemberRefreshTokenRepository orgMemberRefreshTokenRepository;

  public OrgMemberRefreshTokenJpaRepositoryAdapter(OrgMemberRefreshTokenRepository orgMemberRefreshTokenRepository) {
    this.orgMemberRefreshTokenRepository = orgMemberRefreshTokenRepository;
  }

  @Override
  public Optional<OrgMemberRefreshToken> findByTokenHash(String tokenHash) {
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
  public OrgMemberRefreshToken save(OrgMemberRefreshToken token) {
    return orgMemberRefreshTokenRepository.save(token);
  }
}
