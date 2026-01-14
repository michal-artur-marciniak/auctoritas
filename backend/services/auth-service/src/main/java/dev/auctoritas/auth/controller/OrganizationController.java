package dev.auctoritas.auth.controller;

import dev.auctoritas.auth.dto.*;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.service.OrganizationService;
import dev.auctoritas.common.dto.ApiResponse;
import dev.auctoritas.common.enums.OrgMemberRole;
import dev.auctoritas.common.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/org")
@RequiredArgsConstructor
public class OrganizationController {

    private static final String ERROR_ORG_NOT_FOUND = "ORGANIZATION_NOT_FOUND";
    private static final String ERROR_ACCESS_DENIED = "ACCESS_DENIED";
    private static final String ERROR_NOT_ORG_MEMBER = "NOT_ORG_MEMBER";

    private final OrganizationService organizationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OrganizationRegistrationResponse>> register(
            @Valid @RequestBody OrganizationRegistrationRequest req) {
        log.info("Registration request for organization: {} with slug: {}", req.organizationName(), req.slug());

        RegistrationResult result = organizationService.register(req);

        if (!result.success()) {
            log.warn("Organization registration failed: {}", result.message());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(result.message(), "REGISTRATION_FAILED"));
        }

        OrganizationRegistrationResponse response = buildRegistrationResponse(result);
        log.info("Organization registered successfully: {} (ID: {})", result.organizationName(), result.organizationId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organization registered successfully", response));
    }

    @GetMapping("/check-slug")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkSlugAvailability(
            @RequestParam String slug) {
        log.debug("Checking slug availability: {}", slug);

        boolean available = organizationService.isSlugAvailable(slug);
        Map<String, Boolean> result = new HashMap<>();
        result.put("available", available);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{orgId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrganizationDetailsResponse>> getOrganization(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        log.debug("Fetching organization: {}", orgId);

        Organization organization = organizationService.findById(orgId);
        if (organization == null) {
            return ResponseEntity.notFound().build();
        }

        if (!isOrgMember(principal, orgId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this organization", ERROR_NOT_ORG_MEMBER));
        }

        OrganizationDetailsResponse response = buildOrganizationDetails(organization);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{orgId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Organization>> updateOrganization(
            @PathVariable UUID orgId,
            @Valid @RequestBody UpdateOrganizationRequest req,
            @AuthenticationPrincipal JwtPrincipal principal) {
        log.info("Update request for organization: {}", orgId);

        Organization organization = organizationService.findById(orgId);
        if (organization == null) {
            return ResponseEntity.notFound().build();
        }

        if (!isOrgMember(principal, orgId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this organization", ERROR_NOT_ORG_MEMBER));
        }

        if (!hasRequiredRole(principal, orgId, OrgMemberRole.OWNER, OrgMemberRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to update this organization", ERROR_ACCESS_DENIED));
        }

        Organization updated = organizationService.update(orgId, req);
        log.info("Organization updated successfully: {}", orgId);

        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", updated));
    }

    @DeleteMapping("/{orgId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        log.info("Delete request for organization: {}", orgId);

        Organization organization = organizationService.findById(orgId);
        if (organization == null) {
            return ResponseEntity.notFound().build();
        }

        if (!isOrgMember(principal, orgId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not a member of this organization", ERROR_NOT_ORG_MEMBER));
        }

        if (!hasRequiredRole(principal, orgId, OrgMemberRole.OWNER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Only owners can delete an organization", ERROR_ACCESS_DENIED));
        }

        organizationService.delete(orgId);
        log.info("Organization deleted successfully: {}", orgId);

        return ResponseEntity.noContent().build();
    }

    private OrganizationRegistrationResponse buildRegistrationResponse(RegistrationResult result) {
        OrganizationInfo orgInfo = new OrganizationInfo(
                result.organizationId(),
                result.organizationName(),
                result.organizationSlug()
        );

        MemberInfo memberInfo = new MemberInfo(
                result.memberId(),
                result.email(),
                result.name(),
                OrgMemberRole.OWNER.name()
        );

        return new OrganizationRegistrationResponse(orgInfo, memberInfo, null);
    }

    private OrganizationDetailsResponse buildOrganizationDetails(Organization organization) {
        return new OrganizationDetailsResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus(),
                organization.getMembers().size(),
                0,
                organization.getCreatedAt()
        );
    }

    private boolean isOrgMember(JwtPrincipal principal, UUID orgId) {
        if (principal == null || principal.orgId() == null) {
            return false;
        }
        return principal.orgId().equals(orgId.toString());
    }

    private boolean hasRequiredRole(JwtPrincipal principal, UUID orgId, OrgMemberRole... allowedRoles) {
        if (!isOrgMember(principal, orgId)) {
            return false;
        }
        String userRole = principal.role();
        if (userRole == null) {
            return false;
        }
        for (OrgMemberRole allowedRole : allowedRoles) {
            if (allowedRole.name().equals(userRole)) {
                return true;
            }
        }
        return false;
    }
}
