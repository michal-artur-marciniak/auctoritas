package com.example.api.presentation.organization;

import com.example.api.application.organization.OrganizationMemberManagementUseCase;
import com.example.api.application.organization.dto.InvitationResponse;
import com.example.api.application.organization.dto.OrganizationMemberResponse;
import com.example.api.presentation.organization.dto.AcceptInvitationRequestDto;
import com.example.api.presentation.organization.dto.InviteMemberRequestDto;
import com.example.api.presentation.organization.dto.UpdateMemberRoleRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for organization member management.
 */
@RestController
@RequestMapping("/api/v1/customers/orgs/{orgId}/members")
public class OrganizationMemberController {

    private final OrganizationMemberManagementUseCase memberManagementUseCase;

    public OrganizationMemberController(OrganizationMemberManagementUseCase memberManagementUseCase) {
        this.memberManagementUseCase = memberManagementUseCase;
    }

    @PostMapping("/invite")
    public ResponseEntity<InvitationResponse> inviteMember(
            Authentication authentication,
            @PathVariable String orgId,
            @Valid @RequestBody InviteMemberRequestDto dto) {
        final var memberId = (String) authentication.getPrincipal();
        final var response = memberManagementUseCase.inviteMember(dto.toRequest(orgId, memberId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/accept")
    public ResponseEntity<OrganizationMemberResponse> acceptInvitation(
            @PathVariable String orgId,
            @Valid @RequestBody AcceptInvitationRequestDto dto) {
        final var response = memberManagementUseCase.acceptInvitation(dto.toRequest(orgId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{memberId}/role")
    public ResponseEntity<OrganizationMemberResponse> updateRole(
            Authentication authentication,
            @PathVariable String orgId,
            @PathVariable String memberId,
            @Valid @RequestBody UpdateMemberRoleRequestDto dto) {
        final var actorId = (String) authentication.getPrincipal();
        final var response = memberManagementUseCase.changeRole(dto.toRequest(orgId, actorId, memberId));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            Authentication authentication,
            @PathVariable String orgId,
            @PathVariable String memberId) {
        final var actorId = (String) authentication.getPrincipal();
        memberManagementUseCase.removeMember(orgId, actorId, memberId);
        return ResponseEntity.noContent().build();
    }
}
