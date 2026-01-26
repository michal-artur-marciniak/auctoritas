package dev.auctoritas.auth.service;

import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.repository.ApiKeyRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {
  private static final int API_KEY_BYTES = 32;
  private static final String DEFAULT_API_KEY_NAME = "Default Key";
  private static final String LIVE_KEY_PREFIX = "pk_live_";

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
    return createKey(project, DEFAULT_API_KEY_NAME, LIVE_KEY_PREFIX);
  }

  private ApiKeySecret createKey(Project project, String name, String prefix) {
    String rawToken = generateToken(API_KEY_BYTES);
    String rawKey = prefix + rawToken;

    ApiKey apiKey = new ApiKey();
    apiKey.setProject(project);
    apiKey.setName(name);
    apiKey.setPrefix(prefix);
    apiKey.setKeyHash(tokenService.hashToken(rawKey));
    apiKey.setLastUsedAt(LocalDateTime.now());

    ApiKey savedKey = apiKeyRepository.save(apiKey);
    return new ApiKeySecret(savedKey, rawKey);
  }

  private String generateToken(int length) {
    byte[] buffer = new byte[length];
    secureRandom.nextBytes(buffer);
    return encoder.encodeToString(buffer);
  }

  public record ApiKeySecret(ApiKey apiKey, String rawKey) {}
}
