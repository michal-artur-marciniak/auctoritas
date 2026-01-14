package dev.auctoritas.auth.controller;

import dev.auctoritas.auth.dto.*;
import dev.auctoritas.auth.service.OrgMemberService;
import dev.auctoritas.common.dto.ApiResponse;
import dev.auctoritas.common.dto.AuthTokens;
import dev.auctoritas.common.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/org/auth")
@RequiredArgsConstructor
public class OrgAuthController {

    private final OrgMemberService orgMemberService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody OrgLoginRequest req,
            HttpServletRequest request) {
        log.info("Login attempt for email: {}", req.email());

        OrgMemberService.LoginResult result = orgMemberService.login(
                req.email(),
                req.password(),
                null,
                request
        );

        if (result instanceof OrgMemberService.LoginResult.Success success) {
            return ResponseEntity.ok(ApiResponse.success(
                    new LoginResponse(success.member(), success.tokens(), false, null)));
        } else if (result instanceof OrgMemberService.LoginResult.MfaRequired mfa) {
            return ResponseEntity.ok(ApiResponse.success(
                    new LoginResponse(null, null, true, mfa.mfaToken())));
        } else if (result instanceof OrgMemberService.LoginResult.Failure failure) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(failure.message(), failure.errorCode()));
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Unknown login result", "AUTH_LOGIN_UNKNOWN_ERROR"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal JwtPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Not authenticated", "AUTH_NOT_AUTHENTICATED"));
        }

        log.info("Logout for member: {}", principal.subject());
        orgMemberService.logout(
                java.util.UUID.fromString(principal.subject()),
                principal.projectId());

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokens>> refresh(
            @Valid @RequestBody RefreshTokenRequest req,
            HttpServletRequest request) {
        log.info("Token refresh requested");

        AuthTokens tokens = orgMemberService.refresh(req.refreshToken(), request);

        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest req) {
        log.info("MFA verification attempt");

        AuthTokens tokens = orgMemberService.verifyMfa(req.mfaToken(), req.code());

        return ResponseEntity.ok(ApiResponse.success(
                new LoginResponse(null, tokens, false, null)));
    }
}
