package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;
import dev.auctoritas.auth.application.mfa.RegenerateRecoveryCodesResult;
import dev.auctoritas.auth.application.mfa.SetupMfaResult;
import dev.auctoritas.auth.application.mfa.VerifyMfaResult;
import dev.auctoritas.auth.application.port.in.ApplicationPrincipal;
import dev.auctoritas.auth.application.port.in.mfa.DisableOrgMemberMfaUseCase;
import dev.auctoritas.auth.application.port.in.mfa.RegenerateOrgMemberRecoveryCodesUseCase;
import dev.auctoritas.auth.application.port.in.mfa.SetupOrgMemberMfaUseCase;
import dev.auctoritas.auth.application.port.in.mfa.VerifyOrgMemberMfaUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for organization member MFA operations.
 * Handles MFA setup, verification, disable, and recovery code regeneration.
 * Uses org JWT for authentication (no X-API-Key required).
 */
@RestController
@RequestMapping("/api/v1/org/me/mfa")
public class OrganizationMemberMfaController {

  private final SetupOrgMemberMfaUseCase setupMfaUseCase;
  private final VerifyOrgMemberMfaUseCase verifyMfaUseCase;
  private final DisableOrgMemberMfaUseCase disableMfaUseCase;
  private final RegenerateOrgMemberRecoveryCodesUseCase regenerateRecoveryCodesUseCase;

  public OrganizationMemberMfaController(
      SetupOrgMemberMfaUseCase setupMfaUseCase,
      VerifyOrgMemberMfaUseCase verifyMfaUseCase,
      DisableOrgMemberMfaUseCase disableMfaUseCase,
      RegenerateOrgMemberRecoveryCodesUseCase regenerateRecoveryCodesUseCase) {
    this.setupMfaUseCase = setupMfaUseCase;
    this.verifyMfaUseCase = verifyMfaUseCase;
    this.disableMfaUseCase = disableMfaUseCase;
    this.regenerateRecoveryCodesUseCase = regenerateRecoveryCodesUseCase;
  }

  /**
   * Initiates MFA setup for the authenticated organization member.
   * Returns TOTP secret, QR code URL, and recovery codes.
   * MFA is not enabled until verified.
   *
   * @param principal the authenticated organization member
   * @return setup response with secret, QR code, and backup codes
   */
  @PostMapping("/setup")
  public ResponseEntity<SetupMfaResponse> setupMfa(
      @AuthenticationPrincipal OrganizationMemberPrincipal principal) {
    SetupMfaResult result = setupMfaUseCase.setupMfa(toApplicationPrincipal(principal));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new SetupMfaResponse(result.secret(), result.qrCodeUrl(), result.backupCodes()));
  }

  /**
   * Verifies MFA setup for the authenticated organization member.
   * Validates the TOTP code and enables MFA.
   *
   * @param principal the authenticated organization member
   * @param request the verification request containing the TOTP code
   * @return response with recovery codes on success
   */
  @PostMapping("/verify")
  public ResponseEntity<RegenerateRecoveryCodesResponse> verifyMfa(
      @AuthenticationPrincipal OrganizationMemberPrincipal principal,
      @Valid @RequestBody VerifyMfaRequest request) {
    VerifyMfaResult result = verifyMfaUseCase.verifyMfa(toApplicationPrincipal(principal), request.code());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new RegenerateRecoveryCodesResponse(result.recoveryCodes()));
  }

  /**
   * Disables MFA for the authenticated organization member.
   * Requires TOTP code verification before disabling.
   * All recovery codes are deleted when MFA is disabled.
   *
   * @param principal the authenticated organization member
   * @param request the disable request containing the TOTP code
   * @return empty response with 204 status on success
   */
  @DeleteMapping
  public ResponseEntity<Void> disableMfa(
      @AuthenticationPrincipal OrganizationMemberPrincipal principal,
      @Valid @RequestBody DisableMfaRequest request) {
    disableMfaUseCase.disableMfa(toApplicationPrincipal(principal), request.code());
    return ResponseEntity.noContent().build();
  }

  /**
   * Regenerates recovery codes for the authenticated organization member.
   * Requires TOTP code verification before regenerating.
   * Old recovery codes are invalidated and replaced with new ones.
   *
   * @param principal the authenticated organization member
   * @param request the regeneration request containing the TOTP code
   * @return response with new backup codes on success
   */
  @PostMapping("/recovery")
  public ResponseEntity<RegenerateRecoveryCodesResponse> regenerateRecoveryCodes(
      @AuthenticationPrincipal OrganizationMemberPrincipal principal,
      @Valid @RequestBody RegenerateRecoveryCodesRequest request) {
    RegenerateRecoveryCodesResult result = regenerateRecoveryCodesUseCase.regenerateRecoveryCodes(
        toApplicationPrincipal(principal), request.code());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new RegenerateRecoveryCodesResponse(result.backupCodes()));
  }

  private ApplicationPrincipal toApplicationPrincipal(OrganizationMemberPrincipal principal) {
    return new ApplicationPrincipal(
        principal.orgMemberId(),
        principal.orgId(),
        principal.email(),
        principal.role());
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
      List<String> backupCodes) {
  }

  /**
   * Response payload for recovery code regeneration.
   *
   * @param backupCodes the new recovery codes (shown once)
   */
  public record RegenerateRecoveryCodesResponse(
      List<String> backupCodes) {
  }
}
