package dev.auctoritas.auth.api;

import dev.auctoritas.auth.service.OrgAuthService;
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
  public ResponseEntity<OrgLoginResponse> login(@RequestBody OrgLoginRequest request) {
    return ResponseEntity.ok(orgAuthService.login(request));
  }
}
