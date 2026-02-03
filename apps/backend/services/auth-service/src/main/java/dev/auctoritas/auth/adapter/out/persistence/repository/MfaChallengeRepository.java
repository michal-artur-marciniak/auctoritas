package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.mfa.MfaChallenge;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

/**
 * JPA repository for MfaChallenge entities.
 */
@Repository
public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {

  Optional<MfaChallenge> findByToken(String token);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM MfaChallenge c WHERE c.token = :token")
  Optional<MfaChallenge> findByTokenForUpdate(@Param("token") String token);

  List<MfaChallenge> findByUserId(UUID userId);

  List<MfaChallenge> findByMemberId(UUID memberId);

  @Modifying
  @Query("DELETE FROM MfaChallenge c WHERE c.expiresAt < :now")
  int deleteByExpiresAtBefore(@Param("now") Instant now);

  @Modifying
  @Query("UPDATE MfaChallenge c SET c.used = true WHERE c.id = :id")
  void markAsUsed(@Param("id") UUID id);

  void deleteByUserId(UUID userId);

  void deleteByMemberId(UUID memberId);
}
