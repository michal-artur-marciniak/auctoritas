package dev.auctoritas.auth.ports.apikey;

import dev.auctoritas.auth.domain.apikey.ApiKeyStatus;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for API key persistence operations used by application services.
 */
public interface ApiKeyRepositoryPort {
  boolean existsByProjectIdAndName(UUID projectId, String name);

  ApiKey save(ApiKey apiKey);

  Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);

  List<ApiKey> findAllByProjectId(UUID projectId);

  Optional<ApiKey> findByIdAndProjectId(UUID id, UUID projectId);

  void updateStatusByProjectId(UUID projectId, ApiKeyStatus status);
}
