package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.application.port.in.enduser.EndUserLoginResult;
import dev.auctoritas.auth.application.port.in.mfa.CompleteMfaChallengeUseCase;
import dev.auctoritas.auth.application.port.in.mfa.UseRecoveryCodeUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for MFA challenge flow during login.
 * Handles completing challenges with TOTP codes or recovery codes.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class EndUserAuthMfaController {

  private static final String API_KEY_HEADER = "X-API-Key";
  private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
  private final List<String> trustedProxies;

  private final CompleteMfaChallengeUseCase completeMfaChallengeUseCase;
  private final UseRecoveryCodeUseCase useRecoveryCodeUseCase;

  public EndUserAuthMfaController(
      CompleteMfaChallengeUseCase completeMfaChallengeUseCase,
      UseRecoveryCodeUseCase useRecoveryCodeUseCase,
      @Value("${auth.security.trusted-proxies:}") List<String> trustedProxies) {
    this.completeMfaChallengeUseCase = completeMfaChallengeUseCase;
    this.useRecoveryCodeUseCase = useRecoveryCodeUseCase;
    this.trustedProxies = trustedProxies == null
        ? List.of()
        : trustedProxies.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Completes an MFA challenge with a TOTP code.
   * On success, returns the full login response with tokens.
   *
   * @param apiKey the API key for the project
   * @param request the MFA challenge request containing token and code
   * @param httpRequest the HTTP request for IP/user agent extraction
   * @return login response with tokens
   */
  @PostMapping("/login/mfa")
  public ResponseEntity<EndUserLoginResponse> completeMfaChallenge(
      @RequestHeader(value = API_KEY_HEADER) String apiKey,
      @Valid @RequestBody MfaChallengeRequest request,
      HttpServletRequest httpRequest) {
    String ipAddress = resolveIpAddress(httpRequest);
    String userAgent = resolveUserAgent(httpRequest);
    EndUserLoginResult result = completeMfaChallengeUseCase.completeChallenge(
        apiKey, request.mfaToken(), request.code(), ipAddress, userAgent);
    return ResponseEntity.ok(toResponse(result));
  }

  /**
   * Completes an MFA challenge with a recovery code.
   * On success, returns the full login response with tokens.
   * The recovery code is marked as used and cannot be reused.
   *
   * @param apiKey the API key for the project
   * @param request the recovery code request containing token and code
   * @param httpRequest the HTTP request for IP/user agent extraction
   * @return login response with tokens
   */
  @PostMapping("/login/recovery")
  public ResponseEntity<EndUserLoginResponse> useRecoveryCode(
      @RequestHeader(value = API_KEY_HEADER) String apiKey,
      @Valid @RequestBody RecoveryCodeRequest request,
      HttpServletRequest httpRequest) {
    String ipAddress = resolveIpAddress(httpRequest);
    String userAgent = resolveUserAgent(httpRequest);
    EndUserLoginResult result = useRecoveryCodeUseCase.useRecoveryCode(
        apiKey, request.mfaToken(), request.recoveryCode(), ipAddress, userAgent);
    return ResponseEntity.ok(toResponse(result));
  }

  private EndUserLoginResponse toResponse(EndUserLoginResult result) {
    if (Boolean.TRUE.equals(result.mfaRequired())) {
      return EndUserLoginResponse.mfaChallenge(result.mfaToken());
    }
    EndUserLoginResult.EndUserSummary user = result.user();
    if (user == null) {
      throw new IllegalStateException("EndUserLoginResult.user was null when mfaRequired=false");
    }
    return EndUserLoginResponse.success(
        new EndUserLoginResponse.EndUserSummary(
            user.id(),
            user.email(),
            user.name(),
            user.emailVerified()),
        result.accessToken(),
        result.refreshToken());
  }

  private String resolveIpAddress(HttpServletRequest request) {
    if (isFromTrustedProxy(request)) {
      String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
      if (forwarded != null && !forwarded.isBlank()) {
        String[] parts = forwarded.split(",");
        if (parts.length > 0) {
          return parts[0].trim();
        }
      }
    }
    return request.getRemoteAddr();
  }

  private boolean isFromTrustedProxy(HttpServletRequest request) {
    if (trustedProxies.isEmpty()) {
      return false;
    }
    return trustedProxies.contains(request.getRemoteAddr());
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
