package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.application.port.in.oauth.PublicOAuthAuthorizationUseCase;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/oauth")
public class PublicOAuthController {
  private static final String API_KEY_HEADER = "X-API-Key";

  private final PublicOAuthAuthorizationUseCase publicOAuthAuthorizationUseCase;

  public PublicOAuthController(PublicOAuthAuthorizationUseCase publicOAuthAuthorizationUseCase) {
    this.publicOAuthAuthorizationUseCase = publicOAuthAuthorizationUseCase;
  }

  @GetMapping("/{provider}")
  public ResponseEntity<Void> authorizeAlias(
      @PathVariable("provider") String provider,
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @RequestParam(value = "redirect_uri", required = false) String redirectUri) {
    return authorize(provider, apiKey, redirectUri);
  }

  @GetMapping("/{provider}/authorize")
  public ResponseEntity<Void> authorize(
      @PathVariable("provider") String provider,
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @RequestParam(value = "redirect_uri", required = false) String redirectUri) {
    URI location = publicOAuthAuthorizationUseCase.authorize(provider, apiKey, redirectUri);
    return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
  }

  @GetMapping("/{provider}/callback")
  public ResponseEntity<Void> callback(
      @PathVariable("provider") String provider,
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "state", required = false) String state) {
    URI location = publicOAuthAuthorizationUseCase.callback(provider, code, state);
    return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
  }
}
