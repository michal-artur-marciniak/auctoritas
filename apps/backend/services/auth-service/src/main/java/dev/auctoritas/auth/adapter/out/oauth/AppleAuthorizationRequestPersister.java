package dev.auctoritas.auth.adapter.out.oauth;

import dev.auctoritas.auth.application.oauth.OAuthAuthorizationRequestCreateRequest;
import dev.auctoritas.auth.application.oauth.OAuthAuthorizationRequestPersister;
import org.springframework.stereotype.Component;

@Component
public class AppleAuthorizationRequestPersister implements OAuthAuthorizationRequestPersister {
  private static final String PROVIDER = "apple";

  private final OAuthAppleAuthorizationService oauthAppleAuthorizationService;

  public AppleAuthorizationRequestPersister(
      OAuthAppleAuthorizationService oauthAppleAuthorizationService) {
    this.oauthAppleAuthorizationService = oauthAppleAuthorizationService;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public void createAuthorizationRequest(OAuthAuthorizationRequestCreateRequest request) {
    oauthAppleAuthorizationService.createAuthorizationRequest(
        request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
  }
}
