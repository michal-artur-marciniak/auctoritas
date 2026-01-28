package dev.auctoritas.auth.service;

import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.repository.ApiKeyRepository;
import dev.auctoritas.auth.shared.enums.ApiKeyEnvironment;
import dev.auctoritas.auth.shared.enums.ApiKeyStatus;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiKeyService {
  private static final int API_KEY_BYTES = 32;
  private static final String DEFAULT_API_KEY_NAME = "Default Key";
  private static final String LIVE_KEY_PREFIX = "pk_live_";
  private static final String TEST_KEY_PREFIX = "pk_test_";

  private final ApiKeyRepository apiKeyRepository;
  private final TokenService tokenService;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public ApiKeyService(ApiKeyRepository apiKeyRepository, TokenService tokenService) {
    this.apiKeyRepository = apiKeyRepository;
    this.tokenService = tokenService;
  }

  @Transactional
  public ApiKeySecret createDefaultKey(Project project) {
    return createKey(project, DEFAULT_API_KEY_NAME, ApiKeyEnvironment.LIVE);
  }

  @Transactional
  public ApiKeySecret createKey(Project project, String name, ApiKeyEnvironment environment) {
    if (apiKeyRepository.existsByProjectIdAndName(project.getId(), name)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "api_key_name_taken");
    }

    String prefix = resolvePrefix(environment);
    String rawToken = generateToken(API_KEY_BYTES);
    String rawKey = prefix + rawToken;

    ApiKey apiKey = new ApiKey();
    apiKey.setProject(project);
    apiKey.setName(name);
    apiKey.setPrefix(prefix);
    apiKey.setKeyHash(tokenService.hashToken(rawKey));

    try {
      ApiKey savedKey = apiKeyRepository.save(apiKey);
      return new ApiKeySecret(savedKey, rawKey);
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "api_key_name_taken", ex);
    }
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

  private String generateToken(int length) {
    byte[] buffer = new byte[length];
    secureRandom.nextBytes(buffer);
    return encoder.encodeToString(buffer);
  }

  @Transactional
  public void revokeAllByProjectId(UUID projectId) {
    apiKeyRepository.updateStatusByProjectId(projectId, ApiKeyStatus.REVOKED);
  }

  private String resolvePrefix(ApiKeyEnvironment environment) {
    ApiKeyEnvironment resolved = environment == null ? ApiKeyEnvironment.LIVE : environment;
    return resolved == ApiKeyEnvironment.TEST ? TEST_KEY_PREFIX : LIVE_KEY_PREFIX;
  }

  public record ApiKeySecret(ApiKey apiKey, String rawKey) {}
}
