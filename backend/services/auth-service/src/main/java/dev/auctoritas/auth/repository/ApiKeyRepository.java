package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.common.enums.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
  Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);

  Optional<ApiKey> findByProjectIdAndName(UUID projectId, String name);

  boolean existsByProjectIdAndName(UUID projectId, String name);
}
