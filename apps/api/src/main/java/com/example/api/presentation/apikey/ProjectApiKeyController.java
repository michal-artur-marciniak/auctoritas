package com.example.api.presentation.apikey;

import com.example.api.application.apikey.ListProjectApiKeysUseCase;
import com.example.api.application.apikey.RotateProjectApiKeyUseCase;
import com.example.api.application.apikey.dto.ApiKeyResponse;
import com.example.api.application.apikey.dto.RotateApiKeyRequest;
import com.example.api.application.apikey.dto.RotatedApiKeyResponse;
import com.example.api.domain.project.ProjectId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for API key management.
 */
@RestController
@RequestMapping("/api/v1/customers/orgs/{orgId}/projects/{projectId}/keys")
public class ProjectApiKeyController {

    private final ListProjectApiKeysUseCase listProjectApiKeysUseCase;
    private final RotateProjectApiKeyUseCase rotateProjectApiKeyUseCase;

    public ProjectApiKeyController(ListProjectApiKeysUseCase listProjectApiKeysUseCase,
                                   RotateProjectApiKeyUseCase rotateProjectApiKeyUseCase) {
        this.listProjectApiKeysUseCase = listProjectApiKeysUseCase;
        this.rotateProjectApiKeyUseCase = rotateProjectApiKeyUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys(
            @PathVariable String orgId,
            @PathVariable String projectId) {
        final var projectIdObj = ProjectId.of(projectId);
        final var apiKeys = listProjectApiKeysUseCase.execute(projectIdObj);
        return ResponseEntity.ok(apiKeys);
    }

    @PostMapping
    public ResponseEntity<RotatedApiKeyResponse> rotateApiKey(
            @PathVariable String orgId,
            @PathVariable String projectId,
            @Valid @RequestBody RotateApiKeyRequest request) {
        final var projectIdObj = ProjectId.of(projectId);
        final var response = rotateProjectApiKeyUseCase.execute(projectIdObj, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
