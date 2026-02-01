package dev.auctoritas.auth.adapter.out.oauth;

import dev.auctoritas.auth.application.oauth.OAuthCallbackHandleRequest;
import dev.auctoritas.auth.application.oauth.OAuthCallbackHandler;
import org.springframework.stereotype.Component;

@Component
public class FacebookCallbackHandler implements OAuthCallbackHandler {
  private static final String PROVIDER = "facebook";

  private final OAuthFacebookCallbackService oauthFacebookCallbackService;

  public FacebookCallbackHandler(OAuthFacebookCallbackService oauthFacebookCallbackService) {
    this.oauthFacebookCallbackService = oauthFacebookCallbackService;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public String handleCallback(OAuthCallbackHandleRequest request) {
    return oauthFacebookCallbackService.handleCallback(
        request.code(), request.state(), request.callbackUri());
  }
}
