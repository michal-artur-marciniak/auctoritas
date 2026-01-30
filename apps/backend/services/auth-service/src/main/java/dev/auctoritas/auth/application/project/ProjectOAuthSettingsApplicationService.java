package dev.auctoritas.auth.application.project;

import dev.auctoritas.auth.api.ProjectOAuthSettingsRequest;
import dev.auctoritas.auth.domain.project.ProjectOAuthSettingsUpdate;
import dev.auctoritas.auth.domain.project.ProjectOAuthSettingsUpdate.SecretUpdate;
import dev.auctoritas.auth.domain.project.ProjectOAuthSettingsValidator;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.repository.ProjectSettingsRepository;
import dev.auctoritas.auth.security.OrgMemberPrincipal;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Application service that owns Project OAuth settings updates. */
@Service
public class ProjectOAuthSettingsApplicationService {
  private final ProjectRepository projectRepository;
  private final ProjectSettingsRepository projectSettingsRepository;
  private final TextEncryptor oauthClientSecretEncryptor;
  private final ProjectOAuthSettingsValidator oauthSettingsValidator = new ProjectOAuthSettingsValidator();

  public ProjectOAuthSettingsApplicationService(
      ProjectRepository projectRepository,
      ProjectSettingsRepository projectSettingsRepository,
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
      OrgMemberPrincipal principal,
      ProjectOAuthSettingsRequest request) {
    enforceOrgAccess(orgId, principal);
    ProjectSettings settings = loadProject(orgId, projectId).getSettings();

    ProjectOAuthSettingsUpdate update = oauthSettingsValidator.validate(settings, request.config());
    applySecretUpdate(update.googleClientSecret(), settings::setOauthGoogleClientSecretEnc);
    applySecretUpdate(update.githubClientSecret(), settings::setOauthGithubClientSecretEnc);
    applySecretUpdate(update.microsoftClientSecret(), settings::setOauthMicrosoftClientSecretEnc);
    applySecretUpdate(update.facebookClientSecret(), settings::setOauthFacebookClientSecretEnc);
    applySecretUpdate(update.applePrivateKey(), settings::setOauthApplePrivateKeyEnc);

    settings.setOauthConfig(update.oauthConfig());
    return projectSettingsRepository.save(settings);
  }

  private void enforceOrgAccess(UUID orgId, OrgMemberPrincipal principal) {
    if (principal == null) {
      throw new IllegalStateException("Authenticated org member principal is required.");
    }
    if (!orgId.equals(principal.orgId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "org_access_denied");
    }
  }

  private Project loadProject(UUID orgId, UUID projectId) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "project_not_found"));
    if (!orgId.equals(project.getOrganization().getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "project_not_found");
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
