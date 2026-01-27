package dev.auctoritas.auth.api;

import dev.auctoritas.auth.service.OAuthFacebookAuthorizationService;
import dev.auctoritas.auth.service.OAuthFacebookCallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/oauth/facebook")
public class InternalFacebookOAuthController {
  private final OAuthFacebookAuthorizationService oauthFacebookAuthorizationService;
  private final OAuthFacebookCallbackService oauthFacebookCallbackService;

  public InternalFacebookOAuthController(
      OAuthFacebookAuthorizationService oauthFacebookAuthorizationService,
      OAuthFacebookCallbackService oauthFacebookCallbackService) {
    this.oauthFacebookAuthorizationService = oauthFacebookAuthorizationService;
    this.oauthFacebookCallbackService = oauthFacebookCallbackService;
  }

  @PostMapping("/authorize")
  public ResponseEntity<InternalFacebookAuthorizeResponse> authorize(
      @RequestBody InternalFacebookAuthorizeRequest request) {
    String clientId =
        oauthFacebookAuthorizationService.createAuthorizationRequest(
            request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
    return ResponseEntity.ok(new InternalFacebookAuthorizeResponse(clientId));
  }

  @PostMapping("/callback")
  public ResponseEntity<InternalFacebookCallbackResponse> callback(
      @RequestBody InternalFacebookCallbackRequest request) {
    String redirectUrl =
        oauthFacebookCallbackService.handleCallback(
            request.code(), request.state(), request.callbackUri());
    return ResponseEntity.ok(new InternalFacebookCallbackResponse(redirectUrl));
  }
}
