package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshToken;
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
public interface OrganizationMemberRefreshTokenRepository extends JpaRepository<OrganizationMemberRefreshToken, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<OrganizationMemberRefreshToken> findByTokenHash(String tokenHash);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM OrganizationMemberRefreshToken t WHERE t.expiresAt < :now")
  void deleteExpiredTokens(Instant now);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE OrganizationMemberRefreshToken t SET t.revoked = true WHERE t.member.id = :memberId")
  void revokeAllByMemberId(UUID memberId);
}
