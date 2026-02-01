package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.application.port.in.org.OrgAuthUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/org/auth")
public class OrgAuthController {
  private final OrgAuthUseCase orgAuthUseCase;

  public OrgAuthController(OrgAuthUseCase orgAuthUseCase) {
    this.orgAuthUseCase = orgAuthUseCase;
  }

  @PostMapping("/login")
  public ResponseEntity<OrgLoginResponse> login(@Valid @RequestBody OrgLoginRequest request) {
    return ResponseEntity.ok(orgAuthUseCase.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<OrgRefreshResponse> refresh(
      @Valid @RequestBody OrgRefreshRequest request) {
    return ResponseEntity.ok(orgAuthUseCase.refresh(request));
  }
}
