package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.organization.OrganizationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, UUID> {
    Optional<OrganizationInvitation> findByToken(String token);
    Optional<OrganizationInvitation> findByEmailAndOrganizationId(String email, UUID organizationId);
    List<OrganizationInvitation> findByExpiresAtBefore(Instant now);
}
