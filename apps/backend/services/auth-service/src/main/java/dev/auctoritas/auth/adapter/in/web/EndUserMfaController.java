package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.mfa.SetupMfaResult;
import dev.auctoritas.auth.application.port.in.mfa.SetupMfaUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for end-user MFA operations.
 * Handles MFA setup, verification, disable, and recovery code regeneration.
 */
@RestController
@RequestMapping("/api/v1/users/me/mfa")
public class EndUserMfaController {

  private static final String API_KEY_HEADER = "X-API-Key";

  private final SetupMfaUseCase setupMfaUseCase;

  public EndUserMfaController(SetupMfaUseCase setupMfaUseCase) {
    this.setupMfaUseCase = setupMfaUseCase;
  }

  /**
   * Initiates MFA setup for the authenticated end user.
   * Returns TOTP secret, QR code URL, and recovery codes.
   * MFA is not enabled until verified.
   *
   * @param apiKey the API key for the project
   * @param principal the authenticated end user
   * @return setup response with secret, QR code, and backup codes
   */
  @PostMapping("/setup")
  public ResponseEntity<SetupMfaResponse> setupMfa(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @AuthenticationPrincipal EndUserPrincipal principal) {
    SetupMfaResult result = setupMfaUseCase.setupMfa(apiKey, principal);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new SetupMfaResponse(result.secret(), result.qrCodeUrl(), result.backupCodes()));
  }

  /**
   * Response payload for MFA setup.
   *
   * @param secret the plain TOTP secret (Base32 encoded)
   * @param qrCodeUrl the QR code URL for authenticator apps
   * @param backupCodes the recovery codes (shown once)
   */
  public record SetupMfaResponse(
      String secret,
      String qrCodeUrl,
      java.util.List<String> backupCodes) {
  }
}
