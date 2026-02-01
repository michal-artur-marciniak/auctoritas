package dev.auctoritas.auth.adapter.out.oauth;

import dev.auctoritas.auth.application.oauth.OAuthCallbackHandleRequest;
import dev.auctoritas.auth.application.oauth.OAuthCallbackHandler;
import org.springframework.stereotype.Component;

@Component
public class AppleCallbackHandler implements OAuthCallbackHandler {
  private static final String PROVIDER = "apple";

  private final OAuthAppleCallbackService oauthAppleCallbackService;

  public AppleCallbackHandler(OAuthAppleCallbackService oauthAppleCallbackService) {
    this.oauthAppleCallbackService = oauthAppleCallbackService;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public String handleCallback(OAuthCallbackHandleRequest request) {
    return oauthAppleCallbackService.handleCallback(
        request.code(), request.state(), request.callbackUri());
  }
}
