package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.domain.model.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrgMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT m FROM OrganizationMember m JOIN FETCH m.organization WHERE m.id = :id")
    Optional<OrganizationMember> findByIdWithOrganization(@Param("id") UUID id);
}
