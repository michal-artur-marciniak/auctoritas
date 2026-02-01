package dev.auctoritas.auth.adapter.out.oauth;

import dev.auctoritas.auth.application.oauth.OAuthCallbackHandleRequest;
import dev.auctoritas.auth.application.oauth.OAuthCallbackHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleCallbackHandler implements OAuthCallbackHandler {
  private static final String PROVIDER = "google";

  private final OAuthGoogleCallbackService oauthGoogleCallbackService;

  public GoogleCallbackHandler(OAuthGoogleCallbackService oauthGoogleCallbackService) {
    this.oauthGoogleCallbackService = oauthGoogleCallbackService;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public String handleCallback(OAuthCallbackHandleRequest request) {
    return oauthGoogleCallbackService.handleCallback(
        request.code(), request.state(), request.callbackUri());
  }
}
