package dev.auctoritas.auth.api;

import dev.auctoritas.auth.security.OrgMemberPrincipal;
import dev.auctoritas.auth.service.OrgMemberProfileService;
import dev.auctoritas.auth.service.OrganizationRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/org")
public class OrganizationController {
  private final OrganizationRegistrationService organizationRegistrationService;
  private final OrgMemberProfileService orgMemberProfileService;

  public OrganizationController(
      OrganizationRegistrationService organizationRegistrationService,
      OrgMemberProfileService orgMemberProfileService) {
    this.organizationRegistrationService = organizationRegistrationService;
    this.orgMemberProfileService = orgMemberProfileService;
  }

  @PostMapping("/register")
  public ResponseEntity<OrgRegistrationResponse> register(
      @Valid @RequestBody OrgRegistrationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(organizationRegistrationService.register(request));
  }

  /**
   * Get the current authenticated org member's profile.
   * Requires a valid JWT token in the Authorization header.
   */
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/me")
  public ResponseEntity<OrgMemberProfileResponse> getCurrentMember(
      @AuthenticationPrincipal OrgMemberPrincipal principal) {
    if (principal == null) {
      throw new IllegalStateException("Authenticated org member principal is required.");
    }
    return ResponseEntity.ok(orgMemberProfileService.getProfile(principal.orgMemberId()));
  }
}
