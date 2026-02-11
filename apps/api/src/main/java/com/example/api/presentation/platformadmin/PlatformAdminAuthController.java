package com.example.api.presentation.platformadmin;

import com.example.api.application.platformadmin.PlatformAdminLoginUseCase;
import com.example.api.application.platformadmin.RefreshPlatformAdminTokenUseCase;
import com.example.api.application.platformadmin.dto.PlatformAdminAuthResponse;
import com.example.api.presentation.platformadmin.dto.PlatformAdminLoginRequestDto;
import com.example.api.presentation.platformadmin.dto.PlatformAdminRefreshRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for platform admin authentication.
 */
@RestController
@RequestMapping("/api/platform/auth")
public class PlatformAdminAuthController {

    private final PlatformAdminLoginUseCase loginUseCase;
    private final RefreshPlatformAdminTokenUseCase refreshUseCase;

    public PlatformAdminAuthController(PlatformAdminLoginUseCase loginUseCase,
                                       RefreshPlatformAdminTokenUseCase refreshUseCase) {
        this.loginUseCase = loginUseCase;
        this.refreshUseCase = refreshUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<PlatformAdminAuthResponse> login(@Valid @RequestBody PlatformAdminLoginRequestDto dto) {
        final var response = loginUseCase.execute(dto.email(), dto.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<PlatformAdminAuthResponse> refresh(@Valid @RequestBody PlatformAdminRefreshRequestDto dto) {
        final var response = refreshUseCase.execute(dto.refreshToken());
        return ResponseEntity.ok(response);
    }
}
