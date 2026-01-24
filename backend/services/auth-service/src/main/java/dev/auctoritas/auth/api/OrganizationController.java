package dev.auctoritas.auth.api;

import dev.auctoritas.auth.service.OrganizationRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/org")
public class OrganizationController {
  private final OrganizationRegistrationService organizationRegistrationService;

  public OrganizationController(OrganizationRegistrationService organizationRegistrationService) {
    this.organizationRegistrationService = organizationRegistrationService;
  }

  @PostMapping("/register")
  public ResponseEntity<OrgRegistrationResponse> register(
      @RequestBody OrgRegistrationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(organizationRegistrationService.register(request));
  }
}
