package dev.auctoritas.auth.infrastructure.oauth;

import dev.auctoritas.auth.application.oauth.OAuthCallbackHandleRequest;
import dev.auctoritas.auth.application.oauth.OAuthCallbackHandler;
import org.springframework.stereotype.Component;

@Component
public class GitHubCallbackHandler implements OAuthCallbackHandler {
  private static final String PROVIDER = "github";

  private final OAuthGitHubCallbackService oauthGitHubCallbackService;

  public GitHubCallbackHandler(OAuthGitHubCallbackService oauthGitHubCallbackService) {
    this.oauthGitHubCallbackService = oauthGitHubCallbackService;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public String handleCallback(OAuthCallbackHandleRequest request) {
    return oauthGitHubCallbackService.handleCallback(
        request.code(), request.state(), request.callbackUri());
  }
}
