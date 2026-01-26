package dev.auctoritas.auth.api;

import dev.auctoritas.auth.security.OrgMemberPrincipal;
import dev.auctoritas.auth.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/org/{orgId}/projects")
public class ProjectController {
  private final ProjectService projectService;

  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public ResponseEntity<ProjectCreateResponse> createProject(
      @PathVariable UUID orgId,
      @Valid @RequestBody ProjectCreateRequest request,
      @AuthenticationPrincipal OrgMemberPrincipal principal) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(projectService.createProject(orgId, principal, request));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping
  public ResponseEntity<List<ProjectSummaryResponse>> listProjects(
      @PathVariable UUID orgId, @AuthenticationPrincipal OrgMemberPrincipal principal) {
    return ResponseEntity.ok(projectService.listProjects(orgId, principal));
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping("/{projectId}")
  public ResponseEntity<ProjectSummaryResponse> updateProject(
      @PathVariable UUID orgId,
      @PathVariable UUID projectId,
      @Valid @RequestBody ProjectUpdateRequest request,
      @AuthenticationPrincipal OrgMemberPrincipal principal) {
    return ResponseEntity.ok(projectService.updateProject(orgId, projectId, principal, request));
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{projectId}")
  public ResponseEntity<Void> deleteProject(
      @PathVariable UUID orgId,
      @PathVariable UUID projectId,
      @AuthenticationPrincipal OrgMemberPrincipal principal) {
    projectService.deleteProject(orgId, projectId, principal);
    return ResponseEntity.noContent().build();
  }
}
