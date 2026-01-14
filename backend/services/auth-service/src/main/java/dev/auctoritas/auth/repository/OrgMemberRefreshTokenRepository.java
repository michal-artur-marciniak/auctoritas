package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.organization.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgMemberRefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  List<RefreshToken> findByMemberIdAndRevokedFalse(UUID memberId);

  void deleteByMemberIdAndExpiresAtBefore(UUID memberId, Instant cutoff);

  List<RefreshToken> findByMemberId(UUID memberId);
}
