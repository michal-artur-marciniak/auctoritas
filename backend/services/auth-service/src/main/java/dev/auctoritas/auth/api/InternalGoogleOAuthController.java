package dev.auctoritas.auth.api;

import dev.auctoritas.auth.service.OAuthGoogleAuthorizationService;
import dev.auctoritas.auth.service.OAuthGoogleCallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/oauth/google")
public class InternalGoogleOAuthController {
  private final OAuthGoogleAuthorizationService oauthGoogleAuthorizationService;
  private final OAuthGoogleCallbackService oauthGoogleCallbackService;

  public InternalGoogleOAuthController(
      OAuthGoogleAuthorizationService oauthGoogleAuthorizationService,
      OAuthGoogleCallbackService oauthGoogleCallbackService) {
    this.oauthGoogleAuthorizationService = oauthGoogleAuthorizationService;
    this.oauthGoogleCallbackService = oauthGoogleCallbackService;
  }

  @PostMapping("/authorize")
  public ResponseEntity<InternalGoogleAuthorizeResponse> authorize(
      @RequestBody InternalGoogleAuthorizeRequest request) {
    String clientId =
        oauthGoogleAuthorizationService.createAuthorizationRequest(
            request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
    return ResponseEntity.ok(new InternalGoogleAuthorizeResponse(clientId));
  }

  @PostMapping("/callback")
  public ResponseEntity<InternalGoogleCallbackResponse> callback(
      @RequestBody InternalGoogleCallbackRequest request) {
    String redirectUrl =
        oauthGoogleCallbackService.handleCallback(request.code(), request.state(), request.callbackUri());
    return ResponseEntity.ok(new InternalGoogleCallbackResponse(redirectUrl));
  }
}
