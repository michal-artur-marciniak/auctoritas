package dev.auctoritas.auth.application.port.in.project;

import dev.auctoritas.auth.adapter.in.web.ApiKeyCreateRequest;
import dev.auctoritas.auth.adapter.in.web.ApiKeySecretResponse;
import dev.auctoritas.auth.adapter.in.web.ApiKeySummaryResponse;
import dev.auctoritas.auth.adapter.in.web.ProjectAuthSettingsRequest;
import dev.auctoritas.auth.adapter.in.web.ProjectCreateRequest;
import dev.auctoritas.auth.adapter.in.web.ProjectCreateResponse;
import dev.auctoritas.auth.adapter.in.web.ProjectOAuthSettingsRequest;
import dev.auctoritas.auth.adapter.in.web.ProjectPasswordSettingsRequest;
import dev.auctoritas.auth.adapter.in.web.ProjectSessionSettingsRequest;
import dev.auctoritas.auth.adapter.in.web.ProjectSettingsResponse;
import dev.auctoritas.auth.adapter.in.web.ProjectSummaryResponse;
import dev.auctoritas.auth.adapter.in.web.ProjectUpdateRequest;
import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;
import java.util.List;
import java.util.UUID;

/**
 * Use case for Project management operations.
 */
public interface ProjectManagementUseCase {
  ProjectCreateResponse createProject(
      UUID orgId, OrganizationMemberPrincipal principal, ProjectCreateRequest request);

  List<ProjectSummaryResponse> listProjects(UUID orgId, OrganizationMemberPrincipal principal);

  ProjectSummaryResponse updateProject(
      UUID orgId, UUID projectId, OrganizationMemberPrincipal principal, ProjectUpdateRequest request);

  void deleteProject(UUID orgId, UUID projectId, OrganizationMemberPrincipal principal);

  ProjectSettingsResponse getProjectSettings(
      UUID orgId, UUID projectId, OrganizationMemberPrincipal principal);

  ProjectSettingsResponse updatePasswordSettings(
      UUID orgId,
      UUID projectId,
      OrganizationMemberPrincipal principal,
      ProjectPasswordSettingsRequest request);

  ProjectSettingsResponse updateSessionSettings(
      UUID orgId,
      UUID projectId,
      OrganizationMemberPrincipal principal,
      ProjectSessionSettingsRequest request);

  ProjectSettingsResponse updateAuthSettings(
      UUID orgId,
      UUID projectId,
      OrganizationMemberPrincipal principal,
      ProjectAuthSettingsRequest request);

  ProjectSettingsResponse updateOAuthSettings(
      UUID orgId,
      UUID projectId,
      OrganizationMemberPrincipal principal,
      ProjectOAuthSettingsRequest request);

  ApiKeySecretResponse createApiKey(
      UUID orgId, UUID projectId, OrganizationMemberPrincipal principal, ApiKeyCreateRequest request);

  List<ApiKeySummaryResponse> listApiKeys(
      UUID orgId, UUID projectId, OrganizationMemberPrincipal principal);

  void revokeApiKey(
      UUID orgId, UUID projectId, UUID keyId, OrganizationMemberPrincipal principal);
}
