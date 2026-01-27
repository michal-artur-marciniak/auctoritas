package dev.auctoritas.auth.api;

import dev.auctoritas.auth.service.OAuthGoogleAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/oauth/google")
public class InternalGoogleOAuthController {
  private final OAuthGoogleAuthorizationService oauthGoogleAuthorizationService;

  public InternalGoogleOAuthController(OAuthGoogleAuthorizationService oauthGoogleAuthorizationService) {
    this.oauthGoogleAuthorizationService = oauthGoogleAuthorizationService;
  }

  @PostMapping("/authorize")
  public ResponseEntity<InternalGoogleAuthorizeResponse> authorize(
      @RequestBody InternalGoogleAuthorizeRequest request) {
    String clientId =
        oauthGoogleAuthorizationService.createAuthorizationRequest(
            request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
    return ResponseEntity.ok(new InternalGoogleAuthorizeResponse(clientId));
  }
}
