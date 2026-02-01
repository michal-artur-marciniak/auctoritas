package dev.auctoritas.auth.infrastructure.persistence.repository;

import dev.auctoritas.auth.domain.organization.OrganizationMemberSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberSessionRepository extends JpaRepository<OrganizationMemberSession, UUID> {
    Optional<OrganizationMemberSession> findByMemberId(UUID memberId);
    List<OrganizationMemberSession> findByExpiresAtBefore(Instant now);
    void deleteByExpiresAtBefore(Instant now);
}
