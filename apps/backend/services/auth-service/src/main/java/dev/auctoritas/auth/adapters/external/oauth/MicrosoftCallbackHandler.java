package dev.auctoritas.auth.adapters.external.oauth;

import dev.auctoritas.auth.service.oauth.OAuthCallbackHandleRequest;
import dev.auctoritas.auth.service.oauth.OAuthCallbackHandler;
import org.springframework.stereotype.Component;

@Component
public class MicrosoftCallbackHandler implements OAuthCallbackHandler {
  private static final String PROVIDER = "microsoft";

  private final OAuthMicrosoftCallbackService oauthMicrosoftCallbackService;

  public MicrosoftCallbackHandler(OAuthMicrosoftCallbackService oauthMicrosoftCallbackService) {
    this.oauthMicrosoftCallbackService = oauthMicrosoftCallbackService;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public String handleCallback(OAuthCallbackHandleRequest request) {
    return oauthMicrosoftCallbackService.handleCallback(
        request.code(), request.state(), request.callbackUri());
  }
}
