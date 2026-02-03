package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.application.port.in.mfa.CompleteOrgMemberMfaChallengeUseCase;
import dev.auctoritas.auth.application.port.in.mfa.UseOrgMemberRecoveryCodeUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for organization member MFA challenge flow during login.
 * Handles completing challenges with TOTP codes or recovery codes.
 */
@RestController
@RequestMapping("/api/v1/org/auth")
public class OrgAuthMfaController {

  private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

  private final CompleteOrgMemberMfaChallengeUseCase completeMfaChallengeUseCase;
  private final UseOrgMemberRecoveryCodeUseCase useRecoveryCodeUseCase;

  public OrgAuthMfaController(
      CompleteOrgMemberMfaChallengeUseCase completeMfaChallengeUseCase,
      UseOrgMemberRecoveryCodeUseCase useRecoveryCodeUseCase) {
    this.completeMfaChallengeUseCase = completeMfaChallengeUseCase;
    this.useRecoveryCodeUseCase = useRecoveryCodeUseCase;
  }

  /**
   * Completes an MFA challenge with a TOTP code.
   * On success, returns the full login response with tokens.
   *
   * @param request the MFA challenge request containing token and code
   * @param httpRequest the HTTP request for IP/user agent extraction
   * @return login response with tokens
   */
  @PostMapping("/login/mfa")
  public ResponseEntity<OrgLoginResponse> completeMfaChallenge(
      @Valid @RequestBody MfaChallengeRequest request,
      HttpServletRequest httpRequest) {
    String ipAddress = resolveIpAddress(httpRequest);
    String userAgent = resolveUserAgent(httpRequest);
    OrgLoginResponse response = completeMfaChallengeUseCase.completeChallenge(
        request.mfaToken(), request.code(), ipAddress, userAgent);
    return ResponseEntity.ok(response);
  }

  /**
   * Completes an MFA challenge with a recovery code.
   * On success, returns the full login response with tokens.
   * The recovery code is marked as used and cannot be reused.
   *
   * @param request the recovery code request containing token and code
   * @param httpRequest the HTTP request for IP/user agent extraction
   * @return login response with tokens
   */
  @PostMapping("/login/recovery")
  public ResponseEntity<OrgLoginResponse> useRecoveryCode(
      @Valid @RequestBody RecoveryCodeRequest request,
      HttpServletRequest httpRequest) {
    String ipAddress = resolveIpAddress(httpRequest);
    String userAgent = resolveUserAgent(httpRequest);
    OrgLoginResponse response = useRecoveryCodeUseCase.useRecoveryCode(
        request.mfaToken(), request.recoveryCode(), ipAddress, userAgent);
    return ResponseEntity.ok(response);
  }

  private String resolveIpAddress(HttpServletRequest request) {
    String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
    if (forwarded != null && !forwarded.isBlank()) {
      String[] parts = forwarded.split(",");
      if (parts.length > 0) {
        return parts[0].trim();
      }
    }
    return request.getRemoteAddr();
  }

  private String resolveUserAgent(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null) {
      return null;
    }
    String trimmed = userAgent.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
