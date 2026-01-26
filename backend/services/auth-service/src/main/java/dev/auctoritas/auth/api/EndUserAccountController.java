package dev.auctoritas.auth.api;

import dev.auctoritas.auth.security.EndUserPrincipal;
import dev.auctoritas.auth.service.EndUserPasswordChangeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class EndUserAccountController {
  private static final String API_KEY_HEADER = "X-API-Key";

  private final EndUserPasswordChangeService endUserPasswordChangeService;

  public EndUserAccountController(EndUserPasswordChangeService endUserPasswordChangeService) {
    this.endUserPasswordChangeService = endUserPasswordChangeService;
  }

  /**
   * Changes the end-user password. On success, all sessions/refresh tokens are revoked except the
   * current session (best-effort: the most recently issued session/token is kept).
   */
  @PutMapping("/password")
  public ResponseEntity<EndUserPasswordChangeResponse> changePassword(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @AuthenticationPrincipal EndUserPrincipal principal,
      @Valid @RequestBody EndUserPasswordChangeRequest request) {
    return ResponseEntity.ok(endUserPasswordChangeService.changePassword(apiKey, principal, request));
  }
}
