package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.common.enums.ApiKeyStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
  Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);

  Optional<ApiKey> findByProjectIdAndName(UUID projectId, String name);

  boolean existsByProjectIdAndName(UUID projectId, String name);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE ApiKey k SET k.status = :status WHERE k.project.id = :projectId AND k.status <> :status")
  void updateStatusByProjectId(@Param("projectId") UUID projectId, @Param("status") ApiKeyStatus status);
}
