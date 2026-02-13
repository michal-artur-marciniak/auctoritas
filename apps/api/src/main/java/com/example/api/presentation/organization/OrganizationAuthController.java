package com.example.api.presentation.organization;

import com.example.api.application.auth.org.OrgMemberLoginUseCase;
import com.example.api.application.auth.org.RefreshOrgTokenUseCase;
import com.example.api.application.organization.dto.OrgAuthResponse;
import com.example.api.presentation.organization.dto.OrgLoginRequestDto;
import com.example.api.presentation.organization.dto.OrgRefreshRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for org member authentication.
 */
@RestController
@RequestMapping("/api/v1/customers/auth")
public class OrganizationAuthController {

    private final OrgMemberLoginUseCase loginUseCase;
    private final RefreshOrgTokenUseCase refreshUseCase;

    public OrganizationAuthController(OrgMemberLoginUseCase loginUseCase,
                                      RefreshOrgTokenUseCase refreshUseCase) {
        this.loginUseCase = loginUseCase;
        this.refreshUseCase = refreshUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<OrgAuthResponse> login(@Valid @RequestBody OrgLoginRequestDto dto) {
        final var response = loginUseCase.execute(dto.organizationId(), dto.email(), dto.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<OrgAuthResponse> refresh(@Valid @RequestBody OrgRefreshRequestDto dto) {
        final var response = refreshUseCase.execute(dto.refreshToken());
        return ResponseEntity.ok(response);
    }
}
