package dev.auctoritas.auth.infrastructure.oauth;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.oauth.OAuthAuthorizationRequest;
import dev.auctoritas.auth.domain.oauth.OAuthExchangeCode;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.domain.oauth.OAuthAuthorizationRequestRepositoryPort;
import dev.auctoritas.auth.domain.oauth.OAuthExchangeCodeRepositoryPort;
import dev.auctoritas.auth.service.TokenService;
import dev.auctoritas.auth.service.oauth.OAuthAccountLinkingService;
import dev.auctoritas.auth.application.port.out.oauth.OAuthProviderPort;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;
import dev.auctoritas.auth.service.oauth.OAuthTokenExchangeRequest;
import dev.auctoritas.auth.service.oauth.OAuthUserInfo;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OAuthGitHubCallbackService {
  private static final String PROVIDER = "github";

  private final OAuthAuthorizationRequestRepositoryPort oauthAuthorizationRequestRepository;
  private final OAuthExchangeCodeRepositoryPort oauthExchangeCodeRepository;
  private final TokenService tokenService;
  private final OAuthProviderRegistry oauthProviderRegistry;
  private final OAuthAccountLinkingService oauthAccountLinkingService;

  public OAuthGitHubCallbackService(
      OAuthAuthorizationRequestRepositoryPort oauthAuthorizationRequestRepository,
      OAuthExchangeCodeRepositoryPort oauthExchangeCodeRepository,
      TokenService tokenService,
      OAuthProviderRegistry oauthProviderRegistry,
      OAuthAccountLinkingService oauthAccountLinkingService) {
    this.oauthAuthorizationRequestRepository = oauthAuthorizationRequestRepository;
    this.oauthExchangeCodeRepository = oauthExchangeCodeRepository;
    this.tokenService = tokenService;
    this.oauthProviderRegistry = oauthProviderRegistry;
    this.oauthAccountLinkingService = oauthAccountLinkingService;
  }

  @Transactional
  public String handleCallback(String code, String state, String callbackUri) {
    String resolvedCode = requireValue(code, "oauth_code_missing");
    String resolvedState = requireValue(state, "oauth_state_missing");
    String resolvedCallbackUri = requireValue(callbackUri, "oauth_callback_uri_missing");

    String stateHash = tokenService.hashToken(resolvedState);
    OAuthAuthorizationRequest authRequest =
        oauthAuthorizationRequestRepository
            .findByStateHashForUpdate(stateHash)
            .orElseThrow(() -> new DomainValidationException("oauth_state_invalid"));

    if (!PROVIDER.equalsIgnoreCase(authRequest.getProvider())) {
      throw new DomainValidationException("oauth_provider_invalid");
    }

    Instant now = Instant.now();
    if (authRequest.getExpiresAt() == null || authRequest.getExpiresAt().isBefore(now)) {
      oauthAuthorizationRequestRepository.delete(authRequest);
      throw new DomainValidationException("oauth_state_expired");
    }

    Project project = authRequest.getProject();
    if (project == null || project.getId() == null) {
      throw new DomainValidationException("project_not_found");
    }
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new DomainValidationException("project_settings_missing");
    }

    OAuthProviderPort provider = oauthProviderRegistry.require(PROVIDER);
    OAuthUserInfo userInfo =
        provider.exchangeAuthorizationCode(
            settings,
            new OAuthTokenExchangeRequest(resolvedCode, resolvedCallbackUri, authRequest.getCodeVerifier()));

    if (userInfo == null) {
      throw new DomainValidationException("oauth_github_userinfo_failed");
    }
    String providerUserId = requireValue(userInfo.providerUserId(), "oauth_github_userinfo_failed");
    EndUser user =
        oauthAccountLinkingService.linkOrCreateEndUser(
            project,
            PROVIDER,
            providerUserId,
            userInfo.email(),
            userInfo.emailVerified(),
            userInfo.name(),
            "oauth_github_userinfo_failed");

    // Consume the state only after we've successfully linked/created the user.
    oauthAuthorizationRequestRepository.delete(authRequest);

    String rawCode = tokenService.generateOAuthExchangeCode();
    OAuthExchangeCode exchange = new OAuthExchangeCode();
    exchange.setProject(project);
    exchange.setUser(user);
    exchange.setProvider(PROVIDER);
    exchange.setCodeHash(tokenService.hashToken(rawCode));
    exchange.setExpiresAt(tokenService.getOAuthExchangeCodeExpiry());
    oauthExchangeCodeRepository.save(exchange);

    return UriComponentsBuilder.fromUriString(authRequest.getAppRedirectUri())
        .queryParam("auctoritas_code", rawCode)
        .build(true)
        .toUriString();
  }

  private static String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new DomainValidationException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }
}
