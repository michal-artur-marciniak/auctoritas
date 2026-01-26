package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EndUserRefreshTokenRepository extends JpaRepository<EndUserRefreshToken, UUID> {
  Optional<EndUserRefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "update EndUserRefreshToken token set token.revoked = true "
          + "where token.user.id = :userId and token.revoked = false")
  int revokeActiveByUserId(@Param("userId") UUID userId);
}
