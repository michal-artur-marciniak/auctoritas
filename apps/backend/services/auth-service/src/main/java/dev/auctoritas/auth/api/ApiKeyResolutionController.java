package dev.auctoritas.auth.api;

import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api-keys")
public class ApiKeyResolutionController {
  private static final String API_KEY_HEADER = "X-API-Key";

  private final ApiKeyService apiKeyService;

  public ApiKeyResolutionController(ApiKeyService apiKeyService) {
    this.apiKeyService = apiKeyService;
  }

  @PostMapping("/resolve")
  public ResponseEntity<ApiKeyResolveResponse> resolve(
      @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey) {
    ApiKey resolved = apiKeyService.validateActiveKey(apiKey);
    return ResponseEntity.ok(new ApiKeyResolveResponse(resolved.getProject().getId()));
  }
}
