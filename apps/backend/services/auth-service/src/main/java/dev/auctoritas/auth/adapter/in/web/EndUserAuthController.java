package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.application.enduser.EndUserRegistrationCommand;
import dev.auctoritas.auth.application.enduser.EndUserRegistrationResult;
import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.EndUserEmailVerificationService;
import dev.auctoritas.auth.application.EndUserLoginService;
import dev.auctoritas.auth.application.EndUserLogoutService;
import dev.auctoritas.auth.application.EndUserPasswordResetService;
import dev.auctoritas.auth.application.EndUserRefreshService;
import dev.auctoritas.auth.application.EndUserRegistrationService;
import dev.auctoritas.auth.application.OAuthExchangeService;
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

  private final EndUserRegistrationService endUserRegistrationService;
  private final EndUserLoginService endUserLoginService;
  private final EndUserLogoutService endUserLogoutService;
  private final EndUserRefreshService endUserRefreshService;
  private final EndUserPasswordResetService endUserPasswordResetService;
  private final EndUserEmailVerificationService endUserEmailVerificationService;
  private final OAuthExchangeService oauthExchangeService;
  private final List<String> trustedProxies;

  public EndUserAuthController(
      EndUserRegistrationService endUserRegistrationService,
      EndUserLoginService endUserLoginService,
      EndUserLogoutService endUserLogoutService,
      EndUserRefreshService endUserRefreshService,
      EndUserPasswordResetService endUserPasswordResetService,
      EndUserEmailVerificationService endUserEmailVerificationService,
      OAuthExchangeService oauthExchangeService,
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
    return ResponseEntity.ok(endUserLoginService.login(apiKey, request, ipAddress, userAgent));
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
    return ResponseEntity.ok(oauthExchangeService.exchange(apiKey, request, ipAddress, userAgent));
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
}
