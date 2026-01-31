package dev.auctoritas.auth.service.oauth;

import dev.auctoritas.auth.domain.valueobject.Email;
import dev.auctoritas.auth.domain.valueobject.Password;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.oauth.OAuthConnection;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.ports.identity.EndUserRepositoryPort;
import dev.auctoritas.auth.ports.oauth.OAuthConnectionRepositoryPort;
import dev.auctoritas.auth.service.TokenService;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Links OAuth connections to EndUsers.
 * Thin application service - delegates business logic to domain entities.
 */
@Service
public class OAuthAccountLinkingService {
  private final EndUserRepositoryPort endUserRepository;
  private final OAuthConnectionRepositoryPort oauthConnectionRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  public OAuthAccountLinkingService(
      EndUserRepositoryPort endUserRepository,
      OAuthConnectionRepositoryPort oauthConnectionRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService) {
    this.endUserRepository = endUserRepository;
    this.oauthConnectionRepository = oauthConnectionRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
  }

  @Transactional
  public EndUser linkOrCreateEndUser(
      Project project,
      String provider,
      String providerUserId,
      String email,
      Boolean emailVerified,
      String name,
      String userInfoErrorCode) {

    if (project == null || project.getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_not_found");
    }

    String resolvedProvider = requireValue(provider, "oauth_provider_invalid");
    String resolvedProviderUserId = requireValue(providerUserId, userInfoErrorCode);
    UUID projectId = project.getId();

    String normalizedEmail = normalizeEmailOrNull(email);
    String normalizedName = trimToNull(name);
    boolean isEmailVerified = Boolean.TRUE.equals(emailVerified);

    Optional<OAuthConnection> existingConn =
        oauthConnectionRepository.findByProjectIdAndProviderAndProviderUserId(
            projectId, resolvedProvider, resolvedProviderUserId);

    if (existingConn.isPresent()) {
      OAuthConnection conn = existingConn.get();
      if (normalizedEmail != null && !normalizedEmail.equals(conn.getEmail())) {
        conn.setEmail(normalizedEmail);
        oauthConnectionRepository.save(conn);
      }
      EndUser user = lockUser(projectId, conn.getUser());
      updateUserIfNeeded(user, normalizedName, isEmailVerified);
      return user;
    }

    String resolvedEmail = requireValue(normalizedEmail, userInfoErrorCode);

    if (!isEmailVerified && endUserRepository.existsByEmailAndProjectId(resolvedEmail, projectId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "oauth_email_unverified_conflict");
    }

    EndUser user = findOrCreateEndUser(project, resolvedEmail, normalizedName, isEmailVerified);

    OAuthConnection connection = new OAuthConnection();
    connection.setProject(project);
    connection.setUser(user);
    connection.setProvider(resolvedProvider);
    connection.setProviderUserId(resolvedProviderUserId);
    connection.setEmail(resolvedEmail);

    try {
      oauthConnectionRepository.save(connection);
      return user;
    } catch (DataIntegrityViolationException ex) {
      return handleDuplicateConnection(projectId, resolvedProvider, resolvedProviderUserId, resolvedEmail, normalizedName, isEmailVerified);
    }
  }

  private EndUser findOrCreateEndUser(
      Project project, String email, String name, boolean emailVerified) {

    UUID projectId = project.getId();

    if (emailVerified) {
      Optional<EndUser> existing = endUserRepository.findByEmailAndProjectIdForUpdate(email, projectId);
      if (existing.isPresent()) {
        EndUser user = existing.get();
        boolean changed = false;
        if (!user.isEmailVerified()) {
          user.verifyEmail();
          changed = true;
        }
        if (user.getName() == null && name != null) {
          user.updateName(name);
          changed = true;
        }
        return changed ? endUserRepository.save(user) : user;
      }
    }

    return createOAuthEndUser(project, email, name, emailVerified);
  }

  private EndUser createOAuthEndUser(Project project, String email, String name, boolean emailVerified) {
    Email validatedEmail = Email.of(email);
    String randomPassword = tokenService.generateRefreshToken();
    String hashedPassword = passwordEncoder.encode(randomPassword);

    EndUser user = EndUser.create(
        project,
        validatedEmail,
        Password.fromHash(hashedPassword),
        name);

    if (emailVerified) {
      user.verifyEmail();
    }

    return endUserRepository.save(user);
  }

  private EndUser handleDuplicateConnection(
      UUID projectId,
      String provider,
      String providerUserId,
      String email,
      String name,
      boolean emailVerified) {

    OAuthConnection conn =
        oauthConnectionRepository
            .findByProjectIdAndProviderAndProviderUserId(projectId, provider, providerUserId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.CONFLICT, "oauth_connection_conflict"));

    if (!email.equals(conn.getEmail())) {
      conn.setEmail(email);
      oauthConnectionRepository.save(conn);
    }

    EndUser locked = lockUser(projectId, conn.getUser());
    updateUserIfNeeded(locked, name, emailVerified);
    return locked;
  }

  private EndUser lockUser(UUID projectId, EndUser user) {
    if (user == null || user.getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_not_found");
    }
    return endUserRepository
        .findByIdAndProjectIdForUpdate(user.getId(), projectId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_not_found"));
  }

  private void updateUserIfNeeded(EndUser user, String normalizedName, boolean isEmailVerified) {
    boolean changed = false;
    if (isEmailVerified && !user.isEmailVerified()) {
      user.verifyEmail();
      changed = true;
    }
    if (user.getName() == null && normalizedName != null) {
      user.updateName(normalizedName);
      changed = true;
    }
    if (changed) {
      endUserRepository.save(user);
    }
  }

  private static String normalizeEmailOrNull(String email) {
    String trimmed = trimToNull(email);
    return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
  }

  private static String requireValue(String value, String errorCode) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    return trimmed;
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
