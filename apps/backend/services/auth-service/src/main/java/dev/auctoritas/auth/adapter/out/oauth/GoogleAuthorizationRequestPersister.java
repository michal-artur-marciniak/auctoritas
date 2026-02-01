package dev.auctoritas.auth.adapter.out.oauth;

import dev.auctoritas.auth.application.oauth.OAuthAuthorizationRequestCreateRequest;
import dev.auctoritas.auth.application.oauth.OAuthAuthorizationRequestPersister;
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
