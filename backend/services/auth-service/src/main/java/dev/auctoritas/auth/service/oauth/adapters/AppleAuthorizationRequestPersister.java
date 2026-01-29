package dev.auctoritas.auth.service.oauth.adapters;

import dev.auctoritas.auth.service.OAuthAppleAuthorizationService;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizationRequestCreateRequest;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizationRequestPersister;
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
