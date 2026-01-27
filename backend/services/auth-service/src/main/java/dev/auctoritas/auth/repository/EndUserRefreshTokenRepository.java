package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EndUserRefreshTokenRepository extends JpaRepository<EndUserRefreshToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EndUserRefreshToken> findByTokenHash(String tokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EndUserRefreshToken> findTopByUserIdAndRevokedFalseOrderByCreatedAtDesc(UUID userId);

  @Modifying
  @Query(
      "update EndUserRefreshToken token set token.revoked = true "
          + "where token.user.id = :userId and token.revoked = false")
  int revokeActiveByUserId(@Param("userId") UUID userId);

  @Modifying
  @Query(
      "update EndUserRefreshToken token set token.revoked = true "
          + "where token.user.id = :userId and token.revoked = false and token.id <> :excludedId")
  int revokeActiveByUserIdExcludingId(
      @Param("userId") UUID userId, @Param("excludedId") UUID excludedId);
}
