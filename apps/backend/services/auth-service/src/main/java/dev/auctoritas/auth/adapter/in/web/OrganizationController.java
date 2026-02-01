package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;
import dev.auctoritas.auth.application.port.in.org.OrganizationMemberProfileUseCase;
import dev.auctoritas.auth.application.port.in.org.OrganizationRegistrationUseCase;
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
  private final OrganizationRegistrationUseCase organizationRegistrationUseCase;
  private final OrganizationMemberProfileUseCase orgMemberProfileUseCase;

  public OrganizationController(
      OrganizationRegistrationUseCase organizationRegistrationUseCase,
      OrganizationMemberProfileUseCase orgMemberProfileUseCase) {
    this.organizationRegistrationUseCase = organizationRegistrationUseCase;
    this.orgMemberProfileUseCase = orgMemberProfileUseCase;
  }

  @PostMapping("/register")
  public ResponseEntity<OrgRegistrationResponse> register(
      @Valid @RequestBody OrgRegistrationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(organizationRegistrationUseCase.register(request));
  }

  /**
   * Get the current authenticated org member's profile.
   * Requires a valid JWT token in the Authorization header.
   */
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/me")
  public ResponseEntity<OrganizationMemberProfileResponse> getCurrentMember(
      @AuthenticationPrincipal OrganizationMemberPrincipal principal) {
    if (principal == null) {
      throw new IllegalStateException("Authenticated org member principal is required.");
    }
    return ResponseEntity.ok(orgMemberProfileUseCase.getProfile(principal.orgMemberId()));
  }
}
