package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.enduser.EndUserEmailVerificationToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EndUserEmailVerificationTokenRepository
    extends JpaRepository<EndUserEmailVerificationToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EndUserEmailVerificationToken> findByTokenHash(String tokenHash);

  @Query(
      "select count(token) from EndUserEmailVerificationToken token "
          + "where token.user.id = :userId and token.project.id = :projectId and token.createdAt >= :since")
  long countIssuedSince(
      @Param("userId") UUID userId, @Param("projectId") UUID projectId, @Param("since") Instant since);

  @Modifying
  @Query(
      "update EndUserEmailVerificationToken token set token.usedAt = :usedAt "
          + "where token.user.id = :userId and token.usedAt is null")
  int markUsedByUserId(@Param("userId") UUID userId, @Param("usedAt") Instant usedAt);
}
