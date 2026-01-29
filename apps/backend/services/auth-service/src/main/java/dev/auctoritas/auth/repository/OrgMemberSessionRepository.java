package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.organization.OrgMemberSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgMemberSessionRepository extends JpaRepository<OrgMemberSession, UUID> {
    Optional<OrgMemberSession> findByMemberId(UUID memberId);
    List<OrgMemberSession> findByExpiresAtBefore(Instant now);
    void deleteByExpiresAtBefore(Instant now);
}
