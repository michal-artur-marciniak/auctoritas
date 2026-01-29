package dev.auctoritas.auth.service.oauth.adapters;

import dev.auctoritas.auth.service.OAuthMicrosoftAuthorizationService;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizationRequestCreateRequest;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizationRequestPersister;
import org.springframework.stereotype.Component;

@Component
public class MicrosoftAuthorizationRequestPersister implements OAuthAuthorizationRequestPersister {
  private static final String PROVIDER = "microsoft";

  private final OAuthMicrosoftAuthorizationService oauthMicrosoftAuthorizationService;

  public MicrosoftAuthorizationRequestPersister(
      OAuthMicrosoftAuthorizationService oauthMicrosoftAuthorizationService) {
    this.oauthMicrosoftAuthorizationService = oauthMicrosoftAuthorizationService;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public void createAuthorizationRequest(OAuthAuthorizationRequestCreateRequest request) {
    oauthMicrosoftAuthorizationService.createAuthorizationRequest(
        request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
  }
}
