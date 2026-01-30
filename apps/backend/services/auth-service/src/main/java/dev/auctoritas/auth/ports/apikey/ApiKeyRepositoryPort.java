package dev.auctoritas.auth.ports.apikey;

import dev.auctoritas.auth.entity.project.ApiKey;
import java.util.UUID;

/**
 * Port for API key persistence operations used by application services.
 */
public interface ApiKeyRepositoryPort {
  boolean existsByProjectIdAndName(UUID projectId, String name);

  ApiKey save(ApiKey apiKey);
}
