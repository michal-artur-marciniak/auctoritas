package dev.auctoritas.auth.api;

import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.service.ApiKeyService;
import dev.auctoritas.auth.service.OAuthAppleAuthorizationService;
import dev.auctoritas.auth.service.OAuthAppleCallbackService;
import dev.auctoritas.auth.service.OAuthFacebookAuthorizationService;
import dev.auctoritas.auth.service.OAuthFacebookCallbackService;
import dev.auctoritas.auth.service.OAuthGitHubAuthorizationService;
import dev.auctoritas.auth.service.OAuthGitHubCallbackService;
import dev.auctoritas.auth.service.OAuthGoogleAuthorizationService;
import dev.auctoritas.auth.service.OAuthGoogleCallbackService;
import dev.auctoritas.auth.service.OAuthMicrosoftAuthorizationService;
import dev.auctoritas.auth.service.OAuthMicrosoftCallbackService;
import dev.auctoritas.auth.service.TokenService;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeDetails;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeUrlRequest;
import dev.auctoritas.auth.service.oauth.OAuthProvider;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth/oauth")
public class PublicOAuthController {
  private static final String API_KEY_HEADER = "X-API-Key";
  private static final String FORWARDED_HEADER = "Forwarded";
  private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
  private static final String X_FORWARDED_HOST = "X-Forwarded-Host";
  private static final String X_FORWARDED_PORT = "X-Forwarded-Port";

  private final ApiKeyService apiKeyService;
  private final TokenService tokenService;
  private final ProjectRepository projectRepository;
  private final OAuthProviderRegistry oauthProviderRegistry;

  private final OAuthGoogleAuthorizationService oauthGoogleAuthorizationService;
  private final OAuthGoogleCallbackService oauthGoogleCallbackService;
  private final OAuthGitHubAuthorizationService oauthGitHubAuthorizationService;
  private final OAuthGitHubCallbackService oauthGitHubCallbackService;
  private final OAuthMicrosoftAuthorizationService oauthMicrosoftAuthorizationService;
  private final OAuthMicrosoftCallbackService oauthMicrosoftCallbackService;
  private final OAuthFacebookAuthorizationService oauthFacebookAuthorizationService;
  private final OAuthFacebookCallbackService oauthFacebookCallbackService;
  private final OAuthAppleAuthorizationService oauthAppleAuthorizationService;
  private final OAuthAppleCallbackService oauthAppleCallbackService;

  private final String publicApiBaseUrl;
  private final List<String> trustedProxies;

  public PublicOAuthController(
      ApiKeyService apiKeyService,
      TokenService tokenService,
      ProjectRepository projectRepository,
      OAuthProviderRegistry oauthProviderRegistry,
      OAuthGoogleAuthorizationService oauthGoogleAuthorizationService,
      OAuthGoogleCallbackService oauthGoogleCallbackService,
      OAuthGitHubAuthorizationService oauthGitHubAuthorizationService,
      OAuthGitHubCallbackService oauthGitHubCallbackService,
      OAuthMicrosoftAuthorizationService oauthMicrosoftAuthorizationService,
      OAuthMicrosoftCallbackService oauthMicrosoftCallbackService,
      OAuthFacebookAuthorizationService oauthFacebookAuthorizationService,
      OAuthFacebookCallbackService oauthFacebookCallbackService,
      OAuthAppleAuthorizationService oauthAppleAuthorizationService,
      OAuthAppleCallbackService oauthAppleCallbackService,
      @Value("${auth.public-api-base-url:}") String publicApiBaseUrl,
      @Value("${auth.security.trusted-proxies:}") List<String> trustedProxies) {
    this.apiKeyService = apiKeyService;
    this.tokenService = tokenService;
    this.projectRepository = projectRepository;
    this.oauthProviderRegistry = oauthProviderRegistry;
    this.oauthGoogleAuthorizationService = oauthGoogleAuthorizationService;
    this.oauthGoogleCallbackService = oauthGoogleCallbackService;
    this.oauthGitHubAuthorizationService = oauthGitHubAuthorizationService;
    this.oauthGitHubCallbackService = oauthGitHubCallbackService;
    this.oauthMicrosoftAuthorizationService = oauthMicrosoftAuthorizationService;
    this.oauthMicrosoftCallbackService = oauthMicrosoftCallbackService;
    this.oauthFacebookAuthorizationService = oauthFacebookAuthorizationService;
    this.oauthFacebookCallbackService = oauthFacebookCallbackService;
    this.oauthAppleAuthorizationService = oauthAppleAuthorizationService;
    this.oauthAppleCallbackService = oauthAppleCallbackService;
    this.publicApiBaseUrl = trimTrailingSlash(publicApiBaseUrl);
    this.trustedProxies =
        trustedProxies == null
            ? List.of()
            : trustedProxies.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableList());
  }

  @GetMapping("/{provider}/authorize")
  public ResponseEntity<Void> authorize(
      @PathVariable("provider") String provider,
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @RequestParam(value = "redirect_uri", required = false) String redirectUri,
      HttpServletRequest request) {
    String providerName = normalizeProvider(provider);
    OAuthProvider oauthProvider = oauthProviderRegistry.require(providerName);

    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    UUID projectId = resolvedKey.getProject().getId();

    String state = tokenService.generateOAuthState();
    String codeVerifier = tokenService.generateOAuthCodeVerifier();
    persistAuthorizationRequest(providerName, projectId, redirectUri, state, codeVerifier);

    Project project =
        projectRepository
            .findByIdWithSettings(projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "project_not_found"));
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    OAuthAuthorizeDetails details = oauthProvider.getAuthorizeDetails(settings);
    String callbackUri = resolveCallbackUri(providerName, request);
    String codeChallenge = tokenService.hashToken(codeVerifier);
    String authorizeUrl =
        oauthProvider.buildAuthorizeUrl(
            details, new OAuthAuthorizeUrlRequest(callbackUri, state, codeChallenge, "S256"));

    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authorizeUrl)).build();
  }

  @GetMapping("/{provider}/callback")
  public ResponseEntity<Void> callback(
      @PathVariable("provider") String provider,
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "state", required = false) String state,
      HttpServletRequest request) {
    String providerName = normalizeProvider(provider);
    oauthProviderRegistry.require(providerName);

    String callbackUri = resolveCallbackUri(providerName, request);
    String redirectUrl = handleCallback(providerName, code, state, callbackUri);
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
  }

  private void persistAuthorizationRequest(
      String providerName, UUID projectId, String redirectUri, String state, String codeVerifier) {
    switch (providerName) {
      case "google" -> oauthGoogleAuthorizationService.createAuthorizationRequest(
          projectId, redirectUri, state, codeVerifier);
      case "github" -> oauthGitHubAuthorizationService.createAuthorizationRequest(
          projectId, redirectUri, state, codeVerifier);
      case "microsoft" -> oauthMicrosoftAuthorizationService.createAuthorizationRequest(
          projectId, redirectUri, state, codeVerifier);
      case "facebook" -> oauthFacebookAuthorizationService.createAuthorizationRequest(
          projectId, redirectUri, state, codeVerifier);
      case "apple" -> oauthAppleAuthorizationService.createAuthorizationRequest(
          projectId, redirectUri, state, codeVerifier);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_provider_invalid");
    }
  }

  private String handleCallback(String providerName, String code, String state, String callbackUri) {
    return switch (providerName) {
      case "google" -> oauthGoogleCallbackService.handleCallback(code, state, callbackUri);
      case "github" -> oauthGitHubCallbackService.handleCallback(code, state, callbackUri);
      case "microsoft" -> oauthMicrosoftCallbackService.handleCallback(code, state, callbackUri);
      case "facebook" -> oauthFacebookCallbackService.handleCallback(code, state, callbackUri);
      case "apple" -> oauthAppleCallbackService.handleCallback(code, state, callbackUri);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_provider_invalid");
    };
  }

  private String resolveCallbackUri(String providerName, HttpServletRequest request) {
    String baseUrl = resolvePublicBaseUrl(request);
    return baseUrl + "/api/v1/auth/oauth/" + providerName + "/callback";
  }

  private String resolvePublicBaseUrl(HttpServletRequest request) {
    if (isFromTrustedProxy(request)) {
      String forwarded = request.getHeader(FORWARDED_HEADER);
      String fromForwarded = parseForwardedBaseUrl(forwarded);
      if (fromForwarded != null) {
        return fromForwarded;
      }

      String proto = firstHeaderValue(request.getHeader(X_FORWARDED_PROTO));
      String host = firstHeaderValue(request.getHeader(X_FORWARDED_HOST));
      String port = firstHeaderValue(request.getHeader(X_FORWARDED_PORT));
      String fromXForwarded = buildBaseUrl(proto, host, port);
      if (fromXForwarded != null) {
        return fromXForwarded;
      }
    }

    if (publicApiBaseUrl != null && !publicApiBaseUrl.isBlank()) {
      return publicApiBaseUrl;
    }

    String scheme = request.getScheme();
    String host = request.getServerName();
    int port = request.getServerPort();
    boolean includePort =
        port > 0
            && !("http".equalsIgnoreCase(scheme) && port == 80)
            && !("https".equalsIgnoreCase(scheme) && port == 443);
    return includePort ? scheme + "://" + host + ":" + port : scheme + "://" + host;
  }

  private boolean isFromTrustedProxy(HttpServletRequest request) {
    if (trustedProxies.isEmpty()) {
      return false;
    }
    return trustedProxies.contains(request.getRemoteAddr());
  }

  private static String parseForwardedBaseUrl(String forwarded) {
    if (forwarded == null || forwarded.isBlank()) {
      return null;
    }
    String first = forwarded.split(",")[0].trim();
    if (first.isEmpty()) {
      return null;
    }

    String proto = null;
    String host = null;
    for (String part : first.split(";")) {
      String trimmed = part.trim();
      int idx = trimmed.indexOf('=');
      if (idx <= 0) {
        continue;
      }
      String key = trimmed.substring(0, idx).trim().toLowerCase(Locale.ROOT);
      String value = trimmed.substring(idx + 1).trim();
      if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
        value = value.substring(1, value.length() - 1);
      }
      if (key.equals("proto")) {
        proto = value;
      } else if (key.equals("host")) {
        host = value;
      }
    }

    String built = buildBaseUrl(proto, host, null);
    return built == null ? null : built;
  }

  private static String buildBaseUrl(String protoRaw, String hostRaw, String portRaw) {
    if (protoRaw == null || protoRaw.isBlank()) {
      return null;
    }
    if (hostRaw == null || hostRaw.isBlank()) {
      return null;
    }
    String proto = protoRaw.trim();
    String host = hostRaw.trim();
    if (proto.isEmpty() || host.isEmpty()) {
      return null;
    }

    String base = proto + "://" + host;
    if (portRaw != null && !portRaw.isBlank() && !host.contains(":")) {
      String port = portRaw.trim();
      if (!port.isEmpty() && !"80".equals(port) && !"443".equals(port)) {
        base = base + ":" + port;
      }
    }
    return trimTrailingSlash(base);
  }

  private static String firstHeaderValue(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.split(",")[0].trim();
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
