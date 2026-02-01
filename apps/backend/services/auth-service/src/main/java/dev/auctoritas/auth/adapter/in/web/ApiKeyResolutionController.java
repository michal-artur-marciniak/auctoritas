package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.application.port.in.apikey.ApiKeyResolutionUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api-keys")
public class ApiKeyResolutionController {
  private static final String API_KEY_HEADER = "X-API-Key";

  private final ApiKeyResolutionUseCase apiKeyResolutionUseCase;

  public ApiKeyResolutionController(ApiKeyResolutionUseCase apiKeyResolutionUseCase) {
    this.apiKeyResolutionUseCase = apiKeyResolutionUseCase;
  }

  @PostMapping("/resolve")
  public ResponseEntity<ApiKeyResolveResponse> resolve(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey) {
    ApiKey resolved = apiKeyResolutionUseCase.validateActiveKey(apiKey);
    return ResponseEntity.ok(new ApiKeyResolveResponse(resolved.getProject().getId()));
  }
}
