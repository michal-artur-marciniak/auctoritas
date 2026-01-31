package dev.auctoritas.auth.service;

import dev.auctoritas.auth.domain.apikey.ApiKeyStatus;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.ports.apikey.ApiKeyRepositoryPort;
import dev.auctoritas.auth.ports.messaging.DomainEventPublisherPort;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiKeyService {
  private final ApiKeyRepositoryPort apiKeyRepository;
  private final TokenService tokenService;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public ApiKeyService(
      ApiKeyRepositoryPort apiKeyRepository,
      TokenService tokenService,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.apiKeyRepository = apiKeyRepository;
    this.tokenService = tokenService;
    this.domainEventPublisherPort = domainEventPublisherPort;
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

    // Use rich domain method to record usage
    apiKey.recordUsage();
    ApiKey savedApiKey = apiKeyRepository.save(apiKey);

    // Publish and clear domain events
    apiKey.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    apiKey.clearDomainEvents();

    return savedApiKey;
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

    // Use rich domain method to revoke
    apiKey.revoke("manual_revocation");
    apiKeyRepository.save(apiKey);

    // Publish and clear domain events
    apiKey.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    apiKey.clearDomainEvents();
  }

  @Transactional
  public void revokeAllByProjectId(UUID projectId) {
    apiKeyRepository.updateStatusByProjectId(projectId, ApiKeyStatus.REVOKED);
  }
}
