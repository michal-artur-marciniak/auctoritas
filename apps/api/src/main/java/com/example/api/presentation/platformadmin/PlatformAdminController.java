package com.example.api.presentation.platformadmin;

import com.example.api.application.platformadmin.CreatePlatformAdminUseCase;
import com.example.api.application.platformadmin.PlatformAdminResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for platform admin management.
 * Note: This endpoint is intentionally unprotected for initial admin creation.
 * In production, this should be protected or disabled after initial setup.
 */
@RestController
@RequestMapping("/api/platform/admin")
public class PlatformAdminController {

    private final CreatePlatformAdminUseCase createPlatformAdminUseCase;

    public PlatformAdminController(CreatePlatformAdminUseCase createPlatformAdminUseCase) {
        this.createPlatformAdminUseCase = createPlatformAdminUseCase;
    }

    @PostMapping
    public ResponseEntity<PlatformAdminResponse> create(@Valid @RequestBody CreatePlatformAdminRequestDto dto) {
        final var response = createPlatformAdminUseCase.execute(dto.toRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
