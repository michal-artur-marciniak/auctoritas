package dev.auctoritas.auth.application;

import dev.auctoritas.auth.application.port.in.mfa.CheckMfaPolicyUseCase;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.mfa.EndUserMfaRepositoryPort;
import dev.auctoritas.auth.domain.project.MfaPolicy;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for checking MFA policy.
 * Implements UC-008: CheckMfaPolicyUseCase from the PRD.
 */
@Service
public class CheckMfaPolicyService implements CheckMfaPolicyUseCase {

  private final ProjectRepositoryPort projectRepository;
  private final EndUserMfaRepositoryPort endUserMfaRepository;

  public CheckMfaPolicyService(
      ProjectRepositoryPort projectRepository,
      EndUserMfaRepositoryPort endUserMfaRepository) {
    this.projectRepository = projectRepository;
    this.endUserMfaRepository = endUserMfaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public MfaPolicyCheckResult checkMfaPolicy(UUID projectId, UUID userId) {
    // Load project with settings
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new DomainNotFoundException("project_not_found"));

    // Get MFA policy from project settings
    if (project.getSettings() == null) {
      return MfaPolicyCheckResult.mfaDisabled();
    }

    MfaPolicy policy = MfaPolicy.of(
        project.getSettings().isMfaEnabled(),
        project.getSettings().isMfaRequired()
    );

    // Check if user has MFA enabled
    boolean userMfaEnabled = endUserMfaRepository.isEnabledByUserId(userId);

    // Determine result based on policy and user status
    if (!policy.enabled()) {
      // MFA is completely disabled for this project
      return MfaPolicyCheckResult.mfaDisabled();
    }

    if (policy.required()) {
      // MFA is required - check if user is enrolled
      if (userMfaEnabled) {
        return MfaPolicyCheckResult.mfaEnabled(policy);
      } else {
        return MfaPolicyCheckResult.setupRequired(policy);
      }
    }

    // MFA is optional (enabled but not required)
    if (userMfaEnabled) {
      return MfaPolicyCheckResult.mfaEnabled(policy);
    } else {
      return MfaPolicyCheckResult.mfaOptional(policy);
    }
  }
}
