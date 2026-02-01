package dev.auctoritas.auth.application.port.in.apikey;

import dev.auctoritas.auth.domain.project.ApiKey;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case for API key resolution and validation.
 */
public interface ApiKeyResolutionUseCase {
  ApiKey validateActiveKey(String rawKey);
}
