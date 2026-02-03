package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.enduser.EndUserRegistrationCommand;
import dev.auctoritas.auth.application.enduser.EndUserRegistrationResult;
import dev.auctoritas.auth.application.port.in.enduser.EndUserEmailVerificationUseCase;
import dev.auctoritas.auth.application.port.in.enduser.EndUserLoginResult;
import dev.auctoritas.auth.application.port.in.enduser.EndUserLoginUseCase;
import dev.auctoritas.auth.application.port.in.enduser.EndUserLogoutUseCase;
import dev.auctoritas.auth.application.port.in.enduser.EndUserPasswordResetUseCase;
import dev.auctoritas.auth.application.port.in.enduser.EndUserRefreshUseCase;
import dev.auctoritas.auth.application.port.in.enduser.EndUserRegistrationUseCase;
import dev.auctoritas.auth.application.port.in.oauth.OAuthExchangeUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class EndUserAuthController {
  private static final String API_KEY_HEADER = "X-API-Key";
  private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

  private final EndUserRegistrationUseCase endUserRegistrationService;
  private final EndUserLoginUseCase endUserLoginService;
  private final EndUserLogoutUseCase endUserLogoutService;
  private final EndUserRefreshUseCase endUserRefreshService;
  private final EndUserPasswordResetUseCase endUserPasswordResetService;
  private final EndUserEmailVerificationUseCase endUserEmailVerificationService;
  private final OAuthExchangeUseCase oauthExchangeService;
  private final List<String> trustedProxies;

  public EndUserAuthController(
      EndUserRegistrationUseCase endUserRegistrationService,
      EndUserLoginUseCase endUserLoginService,
      EndUserLogoutUseCase endUserLogoutService,
      EndUserRefreshUseCase endUserRefreshService,
      EndUserPasswordResetUseCase endUserPasswordResetService,
      EndUserEmailVerificationUseCase endUserEmailVerificationService,
      OAuthExchangeUseCase oauthExchangeService,
      @Value("${auth.security.trusted-proxies:}") List<String> trustedProxies) {
    this.endUserRegistrationService = endUserRegistrationService;
    this.endUserLoginService = endUserLoginService;
    this.endUserLogoutService = endUserLogoutService;
    this.endUserRefreshService = endUserRefreshService;
    this.endUserPasswordResetService = endUserPasswordResetService;
    this.endUserEmailVerificationService = endUserEmailVerificationService;
    this.oauthExchangeService = oauthExchangeService;
    this.trustedProxies =
        trustedProxies == null
            ? List.of()
            : trustedProxies.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/register")
  public ResponseEntity<EndUserRegistrationResponse> register(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @Valid @RequestBody EndUserRegistrationRequest request,
      HttpServletRequest httpRequest) {
    String ipAddress = resolveIpAddress(httpRequest);
    String userAgent = resolveUserAgent(httpRequest);
    EndUserRegistrationCommand command =
        new EndUserRegistrationCommand(request.email(), request.password(), request.name());
    EndUserRegistrationResult result =
        endUserRegistrationService.register(apiKey, command, ipAddress, userAgent);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(toApiResponse(result));
  }

  @PostMapping("/login")
  public ResponseEntity<EndUserLoginResponse> login(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @Valid @RequestBody EndUserLoginRequest request,
      HttpServletRequest httpRequest) {
    String ipAddress = resolveIpAddress(httpRequest);
    String userAgent = resolveUserAgent(httpRequest);
    EndUserLoginResult result = endUserLoginService.login(apiKey, request, ipAddress, userAgent);
    return ResponseEntity.ok(toLoginResponse(result));
  }

  @PostMapping("/logout")
  public ResponseEntity<EndUserLogoutResponse> logout(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @AuthenticationPrincipal EndUserPrincipal principal) {
    endUserLogoutService.logout(apiKey, principal);
    return ResponseEntity.ok(new EndUserLogoutResponse("Logged out"));
  }

  @PostMapping("/refresh")
  public ResponseEntity<EndUserRefreshResponse> refresh(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @Valid @RequestBody EndUserRefreshRequest request,
      HttpServletRequest httpRequest) {
    String ipAddress = resolveIpAddress(httpRequest);
    String userAgent = resolveUserAgent(httpRequest);
    return ResponseEntity.ok(endUserRefreshService.refresh(apiKey, request, ipAddress, userAgent));
  }

  @PostMapping("/oauth/exchange")
  public ResponseEntity<EndUserLoginResponse> exchangeOAuthCode(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @Valid @RequestBody OAuthExchangeRequest request,
      HttpServletRequest httpRequest) {
    String ipAddress = resolveIpAddress(httpRequest);
    String userAgent = resolveUserAgent(httpRequest);
    EndUserLoginResult result = oauthExchangeService.exchange(apiKey, request, ipAddress, userAgent);
    return ResponseEntity.ok(toLoginResponse(result));
  }

  @PostMapping("/password/forgot")
  public ResponseEntity<EndUserPasswordResetResponse> forgotPassword(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @Valid @RequestBody EndUserPasswordForgotRequest request,
      HttpServletRequest httpRequest) {
    String ipAddress = resolveIpAddress(httpRequest);
    String userAgent = resolveUserAgent(httpRequest);
    return ResponseEntity.ok(endUserPasswordResetService.requestReset(apiKey, request, ipAddress, userAgent));
  }

  @PostMapping("/password/reset")
  public ResponseEntity<EndUserPasswordResetResponse> resetPassword(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @Valid @RequestBody EndUserPasswordResetRequest request) {
    return ResponseEntity.ok(endUserPasswordResetService.resetPassword(apiKey, request));
  }

  @PostMapping("/register/verify-email")
  public ResponseEntity<EndUserEmailVerificationResponse> verifyEmail(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @Valid @RequestBody EndUserEmailVerificationRequest request) {
    return ResponseEntity.ok(endUserEmailVerificationService.verifyEmail(apiKey, request));
  }

  @PostMapping("/register/resend-verification")
  public ResponseEntity<EndUserEmailVerificationResponse> resendVerification(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @Valid @RequestBody EndUserResendVerificationRequest request) {
    return ResponseEntity.ok(endUserEmailVerificationService.resendVerification(apiKey, request));
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

  private EndUserRegistrationResponse toApiResponse(EndUserRegistrationResult result) {
    EndUserRegistrationResult.EndUserSummary summary = result.user();
    return new EndUserRegistrationResponse(
        new EndUserRegistrationResponse.EndUserSummary(
            summary.id(), summary.email(), summary.name(), summary.emailVerified()),
        result.accessToken(),
        result.refreshToken());
  }

  private EndUserLoginResponse toLoginResponse(EndUserLoginResult result) {
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
}
