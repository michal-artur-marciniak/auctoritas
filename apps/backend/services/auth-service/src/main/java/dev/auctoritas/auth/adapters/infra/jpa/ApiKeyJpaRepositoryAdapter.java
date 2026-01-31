package dev.auctoritas.auth.adapters.infra.jpa;

import dev.auctoritas.auth.domain.apikey.ApiKeyStatus;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.ports.apikey.ApiKeyRepositoryPort;
import dev.auctoritas.auth.repository.ApiKeyRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link ApiKeyRepository} via {@link ApiKeyRepositoryPort}.
 */
@Component
public class ApiKeyJpaRepositoryAdapter implements ApiKeyRepositoryPort {
  private final ApiKeyRepository apiKeyRepository;

  public ApiKeyJpaRepositoryAdapter(ApiKeyRepository apiKeyRepository) {
    this.apiKeyRepository = apiKeyRepository;
  }

  @Override
  public boolean existsByProjectIdAndName(UUID projectId, String name) {
    return apiKeyRepository.existsByProjectIdAndName(projectId, name);
  }

  @Override
  public ApiKey save(ApiKey apiKey) {
    return apiKeyRepository.save(apiKey);
  }

  @Override
  public Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status) {
    return apiKeyRepository.findByKeyHashAndStatus(keyHash, status);
  }

  @Override
  public List<ApiKey> findAllByProjectId(UUID projectId) {
    return apiKeyRepository.findAllByProjectId(projectId);
  }

  @Override
  public Optional<ApiKey> findByIdAndProjectId(UUID id, UUID projectId) {
    return apiKeyRepository.findByIdAndProjectId(id, projectId);
  }

  @Override
  public void updateStatusByProjectId(UUID projectId, ApiKeyStatus status) {
    apiKeyRepository.updateStatusByProjectId(projectId, status);
  }
}
