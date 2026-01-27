package dev.auctoritas.auth.api;

import dev.auctoritas.auth.service.OAuthGitHubAuthorizationService;
import dev.auctoritas.auth.service.OAuthGitHubCallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/oauth/github")
public class InternalGitHubOAuthController {
  private final OAuthGitHubAuthorizationService oauthGitHubAuthorizationService;
  private final OAuthGitHubCallbackService oauthGitHubCallbackService;

  public InternalGitHubOAuthController(
      OAuthGitHubAuthorizationService oauthGitHubAuthorizationService,
      OAuthGitHubCallbackService oauthGitHubCallbackService) {
    this.oauthGitHubAuthorizationService = oauthGitHubAuthorizationService;
    this.oauthGitHubCallbackService = oauthGitHubCallbackService;
  }

  @PostMapping("/authorize")
  public ResponseEntity<InternalGitHubAuthorizeResponse> authorize(
      @RequestBody InternalGitHubAuthorizeRequest request) {
    String clientId =
        oauthGitHubAuthorizationService.createAuthorizationRequest(
            request.projectId(), request.redirectUri(), request.state(), request.codeVerifier());
    return ResponseEntity.ok(new InternalGitHubAuthorizeResponse(clientId));
  }

  @PostMapping("/callback")
  public ResponseEntity<InternalGitHubCallbackResponse> callback(
      @RequestBody InternalGitHubCallbackRequest request) {
    String redirectUrl =
        oauthGitHubCallbackService.handleCallback(request.code(), request.state(), request.callbackUri());
    return ResponseEntity.ok(new InternalGitHubCallbackResponse(redirectUrl));
  }
}
