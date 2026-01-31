package dev.auctoritas.auth.infrastructure.oauth;

import dev.auctoritas.auth.service.oauth.OAuthAuthorizationRequestCreateRequest;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizationRequestPersister;
import org.springframework.stereotype.Component;

@Component
public class FacebookAuthorizationRequestPersister implements OAuthAuthorizationRequestPersister {
  private static final String PROVIDER = "facebook";

  private final OAuthFacebookAuthorizationService oauthFacebookAuthorizationService;

  public FacebookAuthorizationRequestPersister(
      OAuthFacebookAuthorizationService oauthFacebookAuthorizationService) {
    this.oauthFacebookAuthorizationService = oauthFacebookAuthorizationService;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public void createAuthorizationRequest(OAuthAuthorizationRequestCreateRequest request) {
    oauthFacebookAuthorizationService.createAuthorizationRequest(
        request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
  }
}
