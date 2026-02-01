package dev.auctoritas.auth.interface.api;

import dev.auctoritas.auth.application.OrgAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/org/auth")
public class OrgAuthController {
  private final OrgAuthService orgAuthService;

  public OrgAuthController(OrgAuthService orgAuthService) {
    this.orgAuthService = orgAuthService;
  }

  @PostMapping("/login")
  public ResponseEntity<OrgLoginResponse> login(@Valid @RequestBody OrgLoginRequest request) {
    return ResponseEntity.ok(orgAuthService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<OrgRefreshResponse> refresh(
      @Valid @RequestBody OrgRefreshRequest request) {
    return ResponseEntity.ok(orgAuthService.refresh(request));
  }
}
