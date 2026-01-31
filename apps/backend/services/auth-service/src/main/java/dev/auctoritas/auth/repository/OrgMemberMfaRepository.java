package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.domain.model.organization.OrgMemberMfa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgMemberMfaRepository extends JpaRepository<OrgMemberMfa, UUID> {
    Optional<OrgMemberMfa> findByMemberId(UUID memberId);
}
