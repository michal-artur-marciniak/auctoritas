package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.shared.enums.ApiKeyStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
  Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);

  Optional<ApiKey> findByProjectIdAndName(UUID projectId, String name);

  boolean existsByProjectIdAndName(UUID projectId, String name);

  Optional<ApiKey> findByIdAndProjectId(UUID id, UUID projectId);

  List<ApiKey> findAllByProjectId(UUID projectId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE ApiKey k SET k.status = :status WHERE k.project.id = :projectId AND k.status <> :status")
  void updateStatusByProjectId(@Param("projectId") UUID projectId, @Param("status") ApiKeyStatus status);
}
