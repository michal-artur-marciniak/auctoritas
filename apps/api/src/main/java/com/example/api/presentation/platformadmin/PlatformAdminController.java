package com.example.api.presentation.platformadmin;

import com.example.api.application.platformadmin.CreatePlatformAdminUseCase;
import com.example.api.application.platformadmin.DeactivatePlatformAdminUseCase;
import com.example.api.application.platformadmin.GetOrganizationDetailsUseCase;
import com.example.api.application.platformadmin.GetPlatformAdminProfileUseCase;
import com.example.api.application.platformadmin.ImpersonateOrganizationUseCase;
import com.example.api.application.platformadmin.ListAllOrganizationsUseCase;
import com.example.api.application.platformadmin.OrganizationDetailsResponse;
import com.example.api.application.platformadmin.OrganizationSummaryResponse;
import com.example.api.application.platformadmin.PlatformAdminResponse;
import com.example.api.application.platformadmin.UpdatePlatformAdminProfileUseCase;
import com.example.api.application.platformadmin.dto.ImpersonationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for platform admin management.
 * Creating platform admins via HTTP requires platform admin authentication.
 * The first admin must be created via CLI: ./gradlew bootRun --args="create-admin <email> <password> <name>"
 */
@RestController
@RequestMapping("/api/platform/admin")
public class PlatformAdminController {

    private final CreatePlatformAdminUseCase createPlatformAdminUseCase;
    private final GetPlatformAdminProfileUseCase getPlatformAdminProfileUseCase;
    private final UpdatePlatformAdminProfileUseCase updatePlatformAdminProfileUseCase;
    private final DeactivatePlatformAdminUseCase deactivatePlatformAdminUseCase;
    private final ListAllOrganizationsUseCase listAllOrganizationsUseCase;
    private final GetOrganizationDetailsUseCase getOrganizationDetailsUseCase;
    private final ImpersonateOrganizationUseCase impersonateOrganizationUseCase;

    public PlatformAdminController(CreatePlatformAdminUseCase createPlatformAdminUseCase,
                                     GetPlatformAdminProfileUseCase getPlatformAdminProfileUseCase,
                                     UpdatePlatformAdminProfileUseCase updatePlatformAdminProfileUseCase,
                                     DeactivatePlatformAdminUseCase deactivatePlatformAdminUseCase,
                                     ListAllOrganizationsUseCase listAllOrganizationsUseCase,
                                     GetOrganizationDetailsUseCase getOrganizationDetailsUseCase,
                                     ImpersonateOrganizationUseCase impersonateOrganizationUseCase) {
        this.createPlatformAdminUseCase = createPlatformAdminUseCase;
        this.getPlatformAdminProfileUseCase = getPlatformAdminProfileUseCase;
        this.updatePlatformAdminProfileUseCase = updatePlatformAdminProfileUseCase;
        this.deactivatePlatformAdminUseCase = deactivatePlatformAdminUseCase;
        this.listAllOrganizationsUseCase = listAllOrganizationsUseCase;
        this.getOrganizationDetailsUseCase = getOrganizationDetailsUseCase;
        this.impersonateOrganizationUseCase = impersonateOrganizationUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<PlatformAdminResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreatePlatformAdminRequestDto dto) {
        final var response = createPlatformAdminUseCase.execute(dto.toRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<PlatformAdminResponse> getProfile(Authentication authentication) {
        final var adminId = authentication.getName();
        final var response = getPlatformAdminProfileUseCase.execute(adminId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<PlatformAdminResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdatePlatformAdminProfileRequestDto dto) {
        final var adminId = authentication.getName();
        final var response = updatePlatformAdminProfileUseCase.execute(adminId, dto.toRequest());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{adminId}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<Void> deactivate(
            Authentication authentication,
            @PathVariable String adminId) {
        deactivatePlatformAdminUseCase.execute(adminId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/organizations")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<List<OrganizationSummaryResponse>> listAllOrganizations(
            Authentication authentication) {
        final var response = listAllOrganizationsUseCase.execute();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organizations/{orgId}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<OrganizationDetailsResponse> getOrganizationDetails(
            Authentication authentication,
            @PathVariable String orgId) {
        final var response = getOrganizationDetailsUseCase.execute(orgId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/organizations/{orgId}/impersonate")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<ImpersonationResponse> impersonateOrganization(
            Authentication authentication,
            @PathVariable String orgId) {
        final var adminId = authentication.getName();
        final var response = impersonateOrganizationUseCase.execute(orgId, adminId);
        return ResponseEntity.ok(response);
    }
}
