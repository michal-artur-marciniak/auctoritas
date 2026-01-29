package dev.auctoritas.auth.service.oauth;

import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.oauth.OAuthConnection;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.repository.EndUserRepository;
import dev.auctoritas.auth.repository.OAuthConnectionRepository;
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

@Service
public class OAuthAccountLinkingService {
  private final EndUserRepository endUserRepository;
  private final OAuthConnectionRepository oauthConnectionRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  public OAuthAccountLinkingService(
      EndUserRepository endUserRepository,
      OAuthConnectionRepository oauthConnectionRepository,
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
      // Do not link to an existing account by an unverified email.
      throw new ResponseStatusException(HttpStatus.CONFLICT, "oauth_email_unverified_conflict");
    }

    EndUser user =
        (isEmailVerified
                ? endUserRepository.findByEmailAndProjectIdForUpdate(resolvedEmail, projectId)
                : Optional.<EndUser>empty())
            .map(
                existing -> {
                  boolean changed = false;
                  if (!Boolean.TRUE.equals(existing.getEmailVerified())) {
                    existing.setEmailVerified(true);
                    changed = true;
                  }
                  if (existing.getName() == null && normalizedName != null) {
                    existing.setName(normalizedName);
                    changed = true;
                  }
                  return changed ? endUserRepository.save(existing) : existing;
                })
            .orElseGet(
                () -> {
                  EndUser created = new EndUser();
                  created.setProject(project);
                  created.setEmail(resolvedEmail);
                  created.setName(normalizedName);
                  created.setEmailVerified(isEmailVerified);
                  // EndUser requires a password hash; OAuth users don't use it.
                  created.setPasswordHash(passwordEncoder.encode(tokenService.generateRefreshToken()));
                  return endUserRepository.save(created);
                });

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
      OAuthConnection conn =
          oauthConnectionRepository
              .findByProjectIdAndProviderAndProviderUserId(projectId, resolvedProvider, resolvedProviderUserId)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.CONFLICT, "oauth_connection_conflict", ex));
      if (!resolvedEmail.equals(conn.getEmail())) {
        conn.setEmail(resolvedEmail);
        oauthConnectionRepository.save(conn);
      }
      EndUser locked = lockUser(projectId, conn.getUser());
      updateUserIfNeeded(locked, normalizedName, isEmailVerified);
      return locked;
    }
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
    if (isEmailVerified && !Boolean.TRUE.equals(user.getEmailVerified())) {
      user.setEmailVerified(true);
      changed = true;
    }
    if (user.getName() == null && normalizedName != null) {
      user.setName(normalizedName);
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
