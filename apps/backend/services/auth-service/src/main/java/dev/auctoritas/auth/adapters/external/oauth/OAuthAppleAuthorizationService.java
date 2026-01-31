package dev.auctoritas.auth.adapters.external.oauth;

import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.oauth.OAuthAuthorizationRequest;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.ports.oauth.OAuthAuthorizationRequestRepositoryPort;
import dev.auctoritas.auth.ports.project.ProjectRepositoryPort;
import dev.auctoritas.auth.service.TokenService;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeDetails;
import dev.auctoritas.auth.ports.oauth.OAuthProviderPort;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthAppleAuthorizationService {
  private static final Duration AUTH_REQUEST_TTL = Duration.ofMinutes(10);
  private static final String PROVIDER = "apple";

  private final ProjectRepositoryPort projectRepository;
  private final OAuthAuthorizationRequestRepositoryPort oauthAuthorizationRequestRepository;
  private final TokenService tokenService;
  private final OAuthProviderRegistry oauthProviderRegistry;

  public OAuthAppleAuthorizationService(
      ProjectRepositoryPort projectRepository,
      OAuthAuthorizationRequestRepositoryPort oauthAuthorizationRequestRepository,
      TokenService tokenService,
      OAuthProviderRegistry oauthProviderRegistry) {
    this.projectRepository = projectRepository;
    this.oauthAuthorizationRequestRepository = oauthAuthorizationRequestRepository;
    this.tokenService = tokenService;
    this.oauthProviderRegistry = oauthProviderRegistry;
  }

  @Transactional
  public String createAuthorizationRequest(
      UUID projectId, String appRedirectUri, String state, String codeVerifier) {
    if (projectId == null) {
      throw new DomainValidationException("project_id_missing");
    }
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new DomainNotFoundException("project_not_found"));
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new DomainValidationException("project_settings_missing");
    }

    String normalizedRedirectUri = validateRedirectUri(appRedirectUri);
    Map<String, Object> oauthConfig =
        settings.getOauthConfig() == null ? Map.of() : settings.getOauthConfig();
    if (!isRedirectUriAllowed(oauthConfig, PROVIDER, normalizedRedirectUri)) {
      throw new DomainValidationException("oauth_redirect_uri_not_allowed");
    }

    OAuthProviderPort provider = oauthProviderRegistry.require(PROVIDER);
    OAuthAuthorizeDetails details = provider.getAuthorizeDetails(settings);

    String trimmedState = state == null ? null : state.trim();
    if (trimmedState == null || trimmedState.isEmpty()) {
      throw new DomainValidationException("oauth_state_missing");
    }
    String trimmedCodeVerifier = codeVerifier == null ? null : codeVerifier.trim();
    if (trimmedCodeVerifier == null || trimmedCodeVerifier.isEmpty()) {
      throw new DomainValidationException("oauth_code_verifier_missing");
    }

    OAuthAuthorizationRequest request = new OAuthAuthorizationRequest();
    request.setProject(project);
    request.setProvider(PROVIDER);
    request.setStateHash(tokenService.hashToken(trimmedState));
    request.setCodeVerifier(trimmedCodeVerifier);
    request.setAppRedirectUri(normalizedRedirectUri);
    request.setExpiresAt(Instant.now().plus(AUTH_REQUEST_TTL));
    oauthAuthorizationRequestRepository.save(request);

    return details.clientId();
  }

  private static boolean isRedirectUriAllowed(
      Map<String, Object> oauthConfig, String provider, String redirectUri) {
    Object providerRaw = oauthConfig.get(provider);
    if (providerRaw instanceof Map<?, ?> m) {
      Object raw = m.get("redirectUris");
      if (raw instanceof List<?> list && containsString(list, redirectUri)) {
        return true;
      }
    }

    Object raw = oauthConfig.get("redirectUris");
    return raw instanceof List<?> list && containsString(list, redirectUri);
  }

  private static boolean containsString(List<?> list, String value) {
    for (Object entry : list) {
      if (entry instanceof String s && value.equals(s)) {
        return true;
      }
    }
    return false;
  }

  private static String validateRedirectUri(String raw) {
    if (raw == null) {
      throw new DomainValidationException("oauth_redirect_uri_missing");
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException("oauth_redirect_uri_missing");
    }
    try {
      URI uri = new URI(trimmed);
      String scheme = uri.getScheme();
      if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
        throw new DomainValidationException("oauth_redirect_uri_invalid");
      }
      if (uri.getHost() == null) {
        throw new DomainValidationException("oauth_redirect_uri_invalid");
      }
      if (uri.getFragment() != null) {
        throw new DomainValidationException("oauth_redirect_uri_invalid");
      }
      return uri.toString();
    } catch (URISyntaxException e) {
      throw new DomainValidationException("oauth_redirect_uri_invalid", e);
    }
  }
}
