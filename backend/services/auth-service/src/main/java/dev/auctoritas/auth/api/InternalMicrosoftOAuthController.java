package dev.auctoritas.auth.api;

import dev.auctoritas.auth.service.OAuthMicrosoftAuthorizationService;
import dev.auctoritas.auth.service.OAuthMicrosoftCallbackService;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/oauth/microsoft")
public class InternalMicrosoftOAuthController {
  private final OAuthMicrosoftAuthorizationService oauthMicrosoftAuthorizationService;
  private final OAuthMicrosoftCallbackService oauthMicrosoftCallbackService;

  public InternalMicrosoftOAuthController(
      OAuthMicrosoftAuthorizationService oauthMicrosoftAuthorizationService,
      OAuthMicrosoftCallbackService oauthMicrosoftCallbackService) {
    this.oauthMicrosoftAuthorizationService = oauthMicrosoftAuthorizationService;
    this.oauthMicrosoftCallbackService = oauthMicrosoftCallbackService;
  }

  @PostMapping("/authorize")
  public ResponseEntity<InternalMicrosoftAuthorizeResponse> authorize(
      @RequestBody InternalMicrosoftAuthorizeRequest request) {
    OAuthAuthorizeDetails details =
        oauthMicrosoftAuthorizationService.createAuthorizationRequest(
            request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
    return ResponseEntity.ok(
        new InternalMicrosoftAuthorizeResponse(
            details.clientId(), details.authorizationEndpoint(), details.scope()));
  }

  @PostMapping("/callback")
  public ResponseEntity<InternalMicrosoftCallbackResponse> callback(
      @RequestBody InternalMicrosoftCallbackRequest request) {
    String redirectUrl =
        oauthMicrosoftCallbackService.handleCallback(request.code(), request.state(), request.callbackUri());
    return ResponseEntity.ok(new InternalMicrosoftCallbackResponse(redirectUrl));
  }
}
