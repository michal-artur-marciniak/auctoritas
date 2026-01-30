package dev.auctoritas.auth.adapters.external.oauth;

import dev.auctoritas.auth.entity.oauth.OAuthAuthorizationRequest;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.OAuthAuthorizationRequestRepository;
import dev.auctoritas.auth.repository.ProjectRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OAuthFacebookAuthorizationService {
  private static final Duration AUTH_REQUEST_TTL = Duration.ofMinutes(10);
  private static final String PROVIDER = "facebook";

  private final ProjectRepository projectRepository;
  private final OAuthAuthorizationRequestRepository oauthAuthorizationRequestRepository;
  private final TokenService tokenService;
  private final OAuthProviderRegistry oauthProviderRegistry;

  public OAuthFacebookAuthorizationService(
      ProjectRepository projectRepository,
      OAuthAuthorizationRequestRepository oauthAuthorizationRequestRepository,
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_id_missing");
    }
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "project_not_found"));
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    String normalizedRedirectUri = validateRedirectUri(appRedirectUri);
    Map<String, Object> oauthConfig = settings.getOauthConfig() == null ? Map.of() : settings.getOauthConfig();
    if (!isRedirectUriAllowed(oauthConfig, PROVIDER, normalizedRedirectUri)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_not_allowed");
    }

    OAuthProviderPort provider = oauthProviderRegistry.require(PROVIDER);
    OAuthAuthorizeDetails details = provider.getAuthorizeDetails(settings);

    if (state == null || state.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_state_missing");
    }
    if (codeVerifier == null || codeVerifier.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_code_verifier_missing");
    }

    OAuthAuthorizationRequest request = new OAuthAuthorizationRequest();
    request.setProject(project);
    request.setProvider(PROVIDER);
    request.setStateHash(tokenService.hashToken(state));
    request.setCodeVerifier(codeVerifier);
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_missing");
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_missing");
    }
    try {
      URI uri = new URI(trimmed);
      String scheme = uri.getScheme();
      if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_invalid");
      }
      if (uri.getHost() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_invalid");
      }
      if (uri.getFragment() != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_invalid");
      }
      return uri.toString();
    } catch (URISyntaxException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_invalid", e);
    }
  }
}
