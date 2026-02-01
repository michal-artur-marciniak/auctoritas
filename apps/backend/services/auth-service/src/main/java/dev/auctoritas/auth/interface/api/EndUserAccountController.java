package dev.auctoritas.auth.interface.api;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.infrastructure.security.EndUserPrincipal;
import dev.auctoritas.auth.application.EndUserPasswordChangeService;
import jakarta.validation.Valid;
import java.util.UUID;
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
  private static final String SESSION_ID_HEADER = "X-Session-Id";

  private final EndUserPasswordChangeService endUserPasswordChangeService;

  public EndUserAccountController(EndUserPasswordChangeService endUserPasswordChangeService) {
    this.endUserPasswordChangeService = endUserPasswordChangeService;
  }

  /**
   * Changes the end-user password. On success, all sessions/refresh tokens are revoked except the
   * current session.
   */
  @PutMapping("/password")
  public ResponseEntity<EndUserPasswordChangeResponse> changePassword(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @RequestHeader(value = SESSION_ID_HEADER, required = false) String sessionId,
      @AuthenticationPrincipal EndUserPrincipal principal,
      @Valid @RequestBody EndUserPasswordChangeRequest request) {
    UUID currentSessionId = parseSessionId(sessionId);
    return ResponseEntity.ok(
        endUserPasswordChangeService.changePassword(apiKey, principal, currentSessionId, request));
  }

  private UUID parseSessionId(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(sessionId.trim());
    } catch (IllegalArgumentException ex) {
      throw new DomainValidationException("session_id_invalid");
    }
  }
}
