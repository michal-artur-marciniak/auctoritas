package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.enduser.EndUserSession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EndUserSessionRepository extends JpaRepository<EndUserSession, UUID> {
  List<EndUserSession> findByUserId(UUID userId);

  void deleteByUserId(UUID userId);

  List<EndUserSession> findByExpiresAtBefore(Instant now);

  void deleteByExpiresAtBefore(Instant now);
}
