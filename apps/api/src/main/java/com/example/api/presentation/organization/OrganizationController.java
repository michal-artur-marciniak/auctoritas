package com.example.api.presentation.organization;

import com.example.api.application.organization.CreateOrganizationUseCase;
import com.example.api.application.organization.dto.OrganizationResponse;
import com.example.api.presentation.organization.dto.CreateOrganizationRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for organization onboarding.
 */
@RestController
@RequestMapping("/api/v1/org")
public class OrganizationController {

    private final CreateOrganizationUseCase createOrganizationUseCase;

    public OrganizationController(CreateOrganizationUseCase createOrganizationUseCase) {
        this.createOrganizationUseCase = createOrganizationUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<OrganizationResponse> register(@Valid @RequestBody CreateOrganizationRequestDto dto) {
        final var response = createOrganizationUseCase.execute(dto.toRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
