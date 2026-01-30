package dev.auctoritas.auth.adapters.infra.jpa;

import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.ports.apikey.ApiKeyRepositoryPort;
import dev.auctoritas.auth.repository.ApiKeyRepository;
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
}
