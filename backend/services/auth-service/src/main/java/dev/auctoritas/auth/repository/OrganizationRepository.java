package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.shared.enums.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Organization> findByStatus(OrganizationStatus status);
}
