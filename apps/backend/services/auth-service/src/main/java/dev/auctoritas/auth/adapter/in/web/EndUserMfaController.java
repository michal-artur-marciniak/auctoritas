package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.mfa.SetupMfaResult;
import dev.auctoritas.auth.application.port.in.mfa.SetupMfaUseCase;
import dev.auctoritas.auth.application.port.in.mfa.VerifyMfaUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  private final VerifyMfaUseCase verifyMfaUseCase;

  public EndUserMfaController(SetupMfaUseCase setupMfaUseCase, VerifyMfaUseCase verifyMfaUseCase) {
    this.setupMfaUseCase = setupMfaUseCase;
    this.verifyMfaUseCase = verifyMfaUseCase;
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
   * Verifies MFA setup for the authenticated end user.
   * Validates the TOTP code and enables MFA.
   *
   * @param apiKey the API key for the project
   * @param principal the authenticated end user
   * @param request the verification request containing the TOTP code
   * @return empty response with 200 status on success
   */
  @PostMapping("/verify")
  public ResponseEntity<Void> verifyMfa(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @AuthenticationPrincipal EndUserPrincipal principal,
      @Valid @RequestBody VerifyMfaRequest request) {
    verifyMfaUseCase.verifyMfa(apiKey, principal, request.code());
    return ResponseEntity.ok().build();
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
