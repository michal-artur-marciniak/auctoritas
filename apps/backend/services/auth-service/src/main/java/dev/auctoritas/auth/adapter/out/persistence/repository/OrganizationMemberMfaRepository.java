package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.organization.OrganizationMemberMfa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberMfaRepository extends JpaRepository<OrganizationMemberMfa, UUID> {
    Optional<OrganizationMemberMfa> findByMemberId(UUID memberId);
}
