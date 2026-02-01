package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.enduser.EndUserPasswordResetToken;
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
public interface EndUserPasswordResetTokenRepository
    extends JpaRepository<EndUserPasswordResetToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EndUserPasswordResetToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "update EndUserPasswordResetToken token set token.usedAt = :usedAt "
          + "where token.user.id = :userId and token.project.id = :projectId and token.usedAt is null")
  int markUsedByUserIdAndProjectId(
      @Param("userId") UUID userId, @Param("projectId") UUID projectId, @Param("usedAt") Instant usedAt);
}
