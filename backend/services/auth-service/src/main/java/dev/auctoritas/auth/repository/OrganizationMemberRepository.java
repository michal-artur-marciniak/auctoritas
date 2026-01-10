package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.organization.OrganizationMember;
import dev.auctoritas.common.enums.OrgMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    Optional<OrganizationMember> findByEmailAndOrganizationId(String email, UUID organizationId);
    boolean existsByEmailAndOrganizationId(String email, UUID organizationId);
    List<OrganizationMember> findByOrganizationId(UUID organizationId);
    List<OrganizationMember> findByStatus(OrgMemberStatus status);
}
