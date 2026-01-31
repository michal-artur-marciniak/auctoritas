package dev.auctoritas.auth.service.oauth;

import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.ports.oauth.OAuthProviderPort;
import dev.auctoritas.auth.ports.project.ProjectRepositoryPort;
import dev.auctoritas.auth.service.ApiKeyService;
import dev.auctoritas.auth.service.TokenService;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Application service for public OAuth authorization and callback flows.
 */
@Service
public class PublicOAuthFlowService {
  private final ApiKeyService apiKeyService;
  private final TokenService tokenService;
  private final ProjectRepositoryPort projectRepository;
  private final OAuthProviderRegistry oauthProviderRegistry;
  private final OAuthAuthorizationRequestPersisterRegistry authorizationRequestPersisterRegistry;
  private final OAuthCallbackHandlerRegistry callbackHandlerRegistry;
  private final String publicApiBaseUrl;

  public PublicOAuthFlowService(
      ApiKeyService apiKeyService,
      TokenService tokenService,
      ProjectRepositoryPort projectRepository,
      OAuthProviderRegistry oauthProviderRegistry,
      OAuthAuthorizationRequestPersisterRegistry authorizationRequestPersisterRegistry,
      OAuthCallbackHandlerRegistry callbackHandlerRegistry,
      @Value("${auth.public-api-base-url:}") String publicApiBaseUrl) {
    this.apiKeyService = apiKeyService;
    this.tokenService = tokenService;
    this.projectRepository = projectRepository;
    this.oauthProviderRegistry = oauthProviderRegistry;
    this.authorizationRequestPersisterRegistry = authorizationRequestPersisterRegistry;
    this.callbackHandlerRegistry = callbackHandlerRegistry;
    this.publicApiBaseUrl = trimTrailingSlash(publicApiBaseUrl);
  }

  public URI authorize(String provider, String apiKey, String redirectUri) {
    String providerName = normalizeProvider(provider);
    OAuthProviderPort oauthProvider = oauthProviderRegistry.require(providerName);

    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    UUID projectId = resolvedKey.getProject().getId();

    String state = tokenService.generateOAuthState();
    String codeVerifier = tokenService.generateOAuthCodeVerifier();
    authorizationRequestPersisterRegistry
        .require(providerName)
        .createAuthorizationRequest(
            new OAuthAuthorizationRequestCreateRequest(projectId, redirectUri, state, codeVerifier));

    Project project =
        projectRepository
            .findByIdWithSettings(projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "project_not_found"));
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    OAuthAuthorizeDetails details = oauthProvider.getAuthorizeDetails(settings);
    String callbackUri = resolveCallbackUri(providerName);
    String codeChallenge = tokenService.hashToken(codeVerifier);
    String authorizeUrl =
        oauthProvider.buildAuthorizeUrl(
            details, new OAuthAuthorizeUrlRequest(callbackUri, state, codeChallenge, "S256"));

    return URI.create(authorizeUrl);
  }

  public URI callback(String provider, String code, String state) {
    String providerName = normalizeProvider(provider);
    oauthProviderRegistry.require(providerName);

    String callbackUri = resolveCallbackUri(providerName);
    String redirectUrl =
        callbackHandlerRegistry
            .require(providerName)
            .handleCallback(new OAuthCallbackHandleRequest(code, state, callbackUri));

    return URI.create(redirectUrl);
  }

  private String resolveCallbackUri(String providerName) {
    UriComponentsBuilder builder;
    if (publicApiBaseUrl != null && !publicApiBaseUrl.isBlank()) {
      builder = UriComponentsBuilder.fromUriString(publicApiBaseUrl);
    } else {
      builder = ServletUriComponentsBuilder.fromCurrentContextPath();
    }

    return builder
        .pathSegment("api", "v1", "auth", "oauth", providerName, "callback")
        .build()
        .toUriString();
  }

  private static String normalizeProvider(String provider) {
    if (provider == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_provider_invalid");
    }
    String trimmed = provider.trim();
    if (trimmed.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_provider_invalid");
    }
    return trimmed.toLowerCase(Locale.ROOT);
  }

  private static String trimTrailingSlash(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.endsWith("/")) {
      return trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }
}
