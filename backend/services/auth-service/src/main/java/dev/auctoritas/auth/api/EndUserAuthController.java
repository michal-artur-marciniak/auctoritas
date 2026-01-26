package dev.auctoritas.auth.api;

import dev.auctoritas.auth.service.EndUserLoginService;
import dev.auctoritas.auth.service.EndUserRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  public EndUserAuthController(
      EndUserRegistrationService endUserRegistrationService,
      EndUserLoginService endUserLoginService) {
    this.endUserRegistrationService = endUserRegistrationService;
    this.endUserLoginService = endUserLoginService;
  }

  @PostMapping("/register")
  public ResponseEntity<EndUserRegistrationResponse> register(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
      @Valid @RequestBody EndUserRegistrationRequest request,
      HttpServletRequest httpRequest) {
    String ipAddress = resolveIpAddress(httpRequest);
    String userAgent = resolveUserAgent(httpRequest);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(endUserRegistrationService.register(apiKey, request, ipAddress, userAgent));
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
