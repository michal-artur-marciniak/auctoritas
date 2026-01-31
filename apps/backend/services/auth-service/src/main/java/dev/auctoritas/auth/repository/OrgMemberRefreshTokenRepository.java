package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.domain.model.organization.OrgMemberRefreshToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgMemberRefreshTokenRepository extends JpaRepository<OrgMemberRefreshToken, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<OrgMemberRefreshToken> findByTokenHash(String tokenHash);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM OrgMemberRefreshToken t WHERE t.expiresAt < :now")
  void deleteExpiredTokens(Instant now);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE OrgMemberRefreshToken t SET t.revoked = true WHERE t.member.id = :memberId")
  void revokeAllByMemberId(UUID memberId);
}
