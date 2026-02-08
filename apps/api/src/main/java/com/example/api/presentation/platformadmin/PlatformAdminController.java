package com.example.api.presentation.platformadmin;

import com.example.api.application.platformadmin.CreatePlatformAdminUseCase;
import com.example.api.application.platformadmin.PlatformAdminResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for platform admin management.
 * Creating platform admins via HTTP requires platform admin authentication.
 * The first admin must be created via CLI: ./gradlew bootRun --args="create-admin <email> <password> <name>"
 */
@RestController
@RequestMapping("/api/platform/admin")
public class PlatformAdminController {

    private final CreatePlatformAdminUseCase createPlatformAdminUseCase;

    public PlatformAdminController(CreatePlatformAdminUseCase createPlatformAdminUseCase) {
        this.createPlatformAdminUseCase = createPlatformAdminUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<PlatformAdminResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreatePlatformAdminRequestDto dto) {
        final var response = createPlatformAdminUseCase.execute(dto.toRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
