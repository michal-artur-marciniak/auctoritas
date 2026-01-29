package dev.auctoritas.auth.service;

import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.repository.ApiKeyRepository;
import dev.auctoritas.auth.shared.enums.ApiKeyStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiKeyService {
  private final ApiKeyRepository apiKeyRepository;
  private final TokenService tokenService;

  public ApiKeyService(ApiKeyRepository apiKeyRepository, TokenService tokenService) {
    this.apiKeyRepository = apiKeyRepository;
    this.tokenService = tokenService;
  }

  @Transactional
  public ApiKey validateActiveKey(String rawKey) {
    if (rawKey == null || rawKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid");
    }
    String keyHash = tokenService.hashToken(rawKey);
    ApiKey apiKey =
        apiKeyRepository
            .findByKeyHashAndStatus(keyHash, ApiKeyStatus.ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid"));
    apiKey.setLastUsedAt(LocalDateTime.now());
    return apiKeyRepository.save(apiKey);
  }

  @Transactional(readOnly = true)
  public List<ApiKey> listKeys(UUID projectId) {
    return apiKeyRepository.findAllByProjectId(projectId);
  }

  @Transactional
  public void revokeKey(UUID projectId, UUID keyId) {
    ApiKey apiKey =
        apiKeyRepository
            .findByIdAndProjectId(keyId, projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "api_key_not_found"));
    if (apiKey.getStatus() != ApiKeyStatus.REVOKED) {
      apiKey.setStatus(ApiKeyStatus.REVOKED);
      apiKeyRepository.save(apiKey);
    }
  }

  @Transactional
  public void revokeAllByProjectId(UUID projectId) {
    apiKeyRepository.updateStatusByProjectId(projectId, ApiKeyStatus.REVOKED);
  }
}
