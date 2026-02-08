package com.example.api.presentation.organization.dto;

import com.example.api.application.organization.dto.AcceptInvitationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * REST request DTO for accepting an invitation.
 */
public record AcceptInvitationRequestDto(
        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
    public AcceptInvitationRequest toRequest(String organizationId) {
        return new AcceptInvitationRequest(organizationId, token, name, password);
    }
}
