package dev.auctoritas.auth.interface.api;

import dev.auctoritas.auth.application.oauth.PublicOAuthFlowService;
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

  private final PublicOAuthFlowService publicOAuthFlowService;

  public PublicOAuthController(PublicOAuthFlowService publicOAuthFlowService) {
    this.publicOAuthFlowService = publicOAuthFlowService;
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
    URI location = publicOAuthFlowService.authorize(provider, apiKey, redirectUri);
    return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
  }

  @GetMapping("/{provider}/callback")
  public ResponseEntity<Void> callback(
      @PathVariable("provider") String provider,
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "state", required = false) String state) {
    URI location = publicOAuthFlowService.callback(provider, code, state);
    return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
  }
}
