package dev.auctoritas.auth.service.oauth.adapters;

import dev.auctoritas.auth.service.OAuthGoogleAuthorizationService;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizationRequestCreateRequest;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizationRequestPersister;
import org.springframework.stereotype.Component;

@Component
public class GoogleAuthorizationRequestPersister implements OAuthAuthorizationRequestPersister {
  private static final String PROVIDER = "google";

  private final OAuthGoogleAuthorizationService oauthGoogleAuthorizationService;

  public GoogleAuthorizationRequestPersister(
      OAuthGoogleAuthorizationService oauthGoogleAuthorizationService) {
    this.oauthGoogleAuthorizationService = oauthGoogleAuthorizationService;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public void createAuthorizationRequest(OAuthAuthorizationRequestCreateRequest request) {
    oauthGoogleAuthorizationService.createAuthorizationRequest(
        request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
  }
}
