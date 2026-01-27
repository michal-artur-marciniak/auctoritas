package dev.auctoritas.auth.api;

import dev.auctoritas.auth.service.OAuthAppleAuthorizationService;
import dev.auctoritas.auth.service.OAuthAppleCallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/oauth/apple")
public class InternalAppleOAuthController {
  private final OAuthAppleAuthorizationService oauthAppleAuthorizationService;
  private final OAuthAppleCallbackService oauthAppleCallbackService;

  public InternalAppleOAuthController(
      OAuthAppleAuthorizationService oauthAppleAuthorizationService,
      OAuthAppleCallbackService oauthAppleCallbackService) {
    this.oauthAppleAuthorizationService = oauthAppleAuthorizationService;
    this.oauthAppleCallbackService = oauthAppleCallbackService;
  }

  @PostMapping("/authorize")
  public ResponseEntity<InternalAppleAuthorizeResponse> authorize(
      @RequestBody InternalAppleAuthorizeRequest request) {
    String clientId =
        oauthAppleAuthorizationService.createAuthorizationRequest(
            request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
    return ResponseEntity.ok(new InternalAppleAuthorizeResponse(clientId));
  }

  @PostMapping("/callback")
  public ResponseEntity<InternalAppleCallbackResponse> callback(
      @RequestBody InternalAppleCallbackRequest request) {
    String redirectUrl =
        oauthAppleCallbackService.handleCallback(request.code(), request.state(), request.callbackUri());
    return ResponseEntity.ok(new InternalAppleCallbackResponse(redirectUrl));
  }
}
