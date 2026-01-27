package dev.auctoritas.auth.service;

import dev.auctoritas.auth.entity.oauth.OAuthAuthorizationRequest;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.OAuthAuthorizationRequestRepository;
import dev.auctoritas.auth.repository.ProjectRepository;
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
public class OAuthGoogleAuthorizationService {
  private static final Duration AUTH_REQUEST_TTL = Duration.ofMinutes(10);
  private static final String PROVIDER = "google";

  private final ProjectRepository projectRepository;
  private final OAuthAuthorizationRequestRepository oauthAuthorizationRequestRepository;
  private final TokenService tokenService;

  public OAuthGoogleAuthorizationService(
      ProjectRepository projectRepository,
      OAuthAuthorizationRequestRepository oauthAuthorizationRequestRepository,
      TokenService tokenService) {
    this.projectRepository = projectRepository;
    this.oauthAuthorizationRequestRepository = oauthAuthorizationRequestRepository;
    this.tokenService = tokenService;
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
    if (!isRedirectUriAllowed(oauthConfig, normalizedRedirectUri)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_not_allowed");
    }

    GoogleConfig google = readGoogleConfig(oauthConfig);
    if (!google.enabled || google.clientId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_not_configured");
    }

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

    return google.clientId;
  }

  private static boolean isRedirectUriAllowed(Map<String, Object> oauthConfig, String redirectUri) {
    Object raw = oauthConfig.get("redirectUris");
    if (!(raw instanceof List<?> list)) {
      return false;
    }
    for (Object entry : list) {
      if (entry instanceof String s && redirectUri.equals(s)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private static GoogleConfig readGoogleConfig(Map<String, Object> oauthConfig) {
    Object googleObj = oauthConfig.get("google");
    if (!(googleObj instanceof Map<?, ?> googleRaw)) {
      return new GoogleConfig(false, null);
    }
    Object enabledObj = ((Map<String, Object>) googleRaw).get("enabled");
    boolean enabled = enabledObj instanceof Boolean b && b;

    Object clientIdObj = ((Map<String, Object>) googleRaw).get("clientId");
    String clientId = clientIdObj == null ? null : clientIdObj.toString().trim();
    if (clientId != null && clientId.isEmpty()) {
      clientId = null;
    }
    return new GoogleConfig(enabled, clientId);
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

  private record GoogleConfig(boolean enabled, String clientId) {}
}
