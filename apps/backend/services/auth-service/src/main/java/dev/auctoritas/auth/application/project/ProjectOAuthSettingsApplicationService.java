package dev.auctoritas.auth.application.project;

import dev.auctoritas.auth.api.ProjectOAuthSettingsRequest;
import dev.auctoritas.auth.domain.exception.DomainForbiddenException;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.model.project.ProjectOAuthSettingsUpdate;
import dev.auctoritas.auth.domain.model.project.ProjectOAuthSettingsUpdate.SecretUpdate;
import dev.auctoritas.auth.domain.model.project.ProjectOAuthSettingsValidator;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.domain.model.project.ProjectRepositoryPort;
import dev.auctoritas.auth.domain.model.project.ProjectSettingsRepositoryPort;
import dev.auctoritas.auth.security.OrganizationMemberPrincipal;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service that owns Project OAuth settings updates. */
@Service
public class ProjectOAuthSettingsApplicationService {
  private final ProjectRepositoryPort projectRepository;
  private final ProjectSettingsRepositoryPort projectSettingsRepository;
  private final TextEncryptor oauthClientSecretEncryptor;
  private final ProjectOAuthSettingsValidator oauthSettingsValidator = new ProjectOAuthSettingsValidator();

  public ProjectOAuthSettingsApplicationService(
      ProjectRepositoryPort projectRepository,
      ProjectSettingsRepositoryPort projectSettingsRepository,
      TextEncryptor oauthClientSecretEncryptor) {
    this.projectRepository = projectRepository;
    this.projectSettingsRepository = projectSettingsRepository;
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
  }

  /** Updates OAuth settings for a project and persists encrypted secrets. */
  @Transactional
  public ProjectSettings updateOAuthSettings(
      UUID orgId,
      UUID projectId,
      OrganizationMemberPrincipal principal,
      ProjectOAuthSettingsRequest request) {
    enforceOrgAccess(orgId, principal);
    ProjectSettings settings = loadProject(orgId, projectId).getSettings();

    ProjectOAuthSettingsUpdate update = oauthSettingsValidator.validate(settings, request.config());

    // Apply encrypted secrets using entity methods
    applySecretUpdate(update.googleClientSecret(), settings::setOauthGoogleClientSecretEnc);
    applySecretUpdate(update.githubClientSecret(), settings::setOauthGithubClientSecretEnc);
    applySecretUpdate(update.microsoftClientSecret(), settings::setOauthMicrosoftClientSecretEnc);
    applySecretUpdate(update.facebookClientSecret(), settings::setOauthFacebookClientSecretEnc);
    applySecretUpdate(update.applePrivateKey(), settings::setOauthApplePrivateKeyEnc);

    // Update OAuth config using entity method
    settings.updateOauthConfig(update.oauthConfig());

    return projectSettingsRepository.save(settings);
  }

  private void enforceOrgAccess(UUID orgId, OrganizationMemberPrincipal principal) {
    if (principal == null) {
      throw new IllegalStateException("Authenticated org member principal is required.");
    }
    if (!orgId.equals(principal.orgId())) {
      throw new DomainForbiddenException("org_access_denied");
    }
  }

  private Project loadProject(UUID orgId, UUID projectId) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new DomainNotFoundException("project_not_found"));
    if (!orgId.equals(project.getOrganization().getId())) {
      throw new DomainNotFoundException("project_not_found");
    }
    return project;
  }

  private void applySecretUpdate(SecretUpdate update, Consumer<String> writer) {
    if (update == null || !update.provided()) {
      return;
    }
    if (update.value() == null) {
      writer.accept(null);
      return;
    }
    writer.accept(oauthClientSecretEncryptor.encrypt(update.value()));
  }
}
