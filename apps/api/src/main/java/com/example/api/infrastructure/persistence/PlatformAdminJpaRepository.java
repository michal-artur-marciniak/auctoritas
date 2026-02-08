package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for platform admin entities.
 */
@Repository
public interface PlatformAdminJpaRepository extends JpaRepository<PlatformAdminJpaEntity, String> {

    Optional<PlatformAdminJpaEntity> findByEmail(String email);

    long countByStatus(String status);
}
