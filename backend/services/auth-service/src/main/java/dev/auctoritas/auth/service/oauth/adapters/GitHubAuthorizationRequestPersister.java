package dev.auctoritas.auth.service.oauth.adapters;

import dev.auctoritas.auth.service.OAuthGitHubAuthorizationService;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizationRequestCreateRequest;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizationRequestPersister;
import org.springframework.stereotype.Component;

@Component
public class GitHubAuthorizationRequestPersister implements OAuthAuthorizationRequestPersister {
  private static final String PROVIDER = "github";

  private final OAuthGitHubAuthorizationService oauthGitHubAuthorizationService;

  public GitHubAuthorizationRequestPersister(
      OAuthGitHubAuthorizationService oauthGitHubAuthorizationService) {
    this.oauthGitHubAuthorizationService = oauthGitHubAuthorizationService;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public void createAuthorizationRequest(OAuthAuthorizationRequestCreateRequest request) {
    oauthGitHubAuthorizationService.createAuthorizationRequest(
        request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
  }
}
