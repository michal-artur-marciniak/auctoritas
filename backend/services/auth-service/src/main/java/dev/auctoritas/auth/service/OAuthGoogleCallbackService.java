package dev.auctoritas.auth.service;

import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.oauth.OAuthAuthorizationRequest;
import dev.auctoritas.auth.entity.oauth.OAuthConnection;
import dev.auctoritas.auth.entity.oauth.OAuthExchangeCode;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserRepository;
import dev.auctoritas.auth.repository.OAuthAuthorizationRequestRepository;
import dev.auctoritas.auth.repository.OAuthConnectionRepository;
import dev.auctoritas.auth.repository.OAuthExchangeCodeRepository;
import dev.auctoritas.auth.service.oauth.OAuthProvider;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;
import dev.auctoritas.auth.service.oauth.OAuthTokenExchangeRequest;
import dev.auctoritas.auth.service.oauth.OAuthUserInfo;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OAuthGoogleCallbackService {
  private static final String PROVIDER = "google";

  private final OAuthAuthorizationRequestRepository oauthAuthorizationRequestRepository;
  private final EndUserRepository endUserRepository;
  private final OAuthConnectionRepository oauthConnectionRepository;
  private final OAuthExchangeCodeRepository oauthExchangeCodeRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final OAuthProviderRegistry oauthProviderRegistry;

  public OAuthGoogleCallbackService(
      OAuthAuthorizationRequestRepository oauthAuthorizationRequestRepository,
      EndUserRepository endUserRepository,
      OAuthConnectionRepository oauthConnectionRepository,
      OAuthExchangeCodeRepository oauthExchangeCodeRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      OAuthProviderRegistry oauthProviderRegistry) {
    this.oauthAuthorizationRequestRepository = oauthAuthorizationRequestRepository;
    this.endUserRepository = endUserRepository;
    this.oauthConnectionRepository = oauthConnectionRepository;
    this.oauthExchangeCodeRepository = oauthExchangeCodeRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.oauthProviderRegistry = oauthProviderRegistry;
  }

  @Transactional
  public String handleCallback(String code, String state, String callbackUri) {
    String resolvedCode = requireValue(code, "oauth_code_missing");
    String resolvedState = requireValue(state, "oauth_state_missing");
    String resolvedCallbackUri = requireValue(callbackUri, "oauth_callback_uri_missing");

    String stateHash = tokenService.hashToken(resolvedState);
    OAuthAuthorizationRequest authRequest =
        oauthAuthorizationRequestRepository
            .findByStateHashForUpdate(stateHash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_state_invalid"));

    if (!PROVIDER.equalsIgnoreCase(authRequest.getProvider())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_provider_invalid");
    }

    Instant now = Instant.now();
    if (authRequest.getExpiresAt() == null || authRequest.getExpiresAt().isBefore(now)) {
      oauthAuthorizationRequestRepository.delete(authRequest);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_state_expired");
    }

    Project project = authRequest.getProject();
    if (project == null || project.getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_not_found");
    }
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "project_settings_missing");
    }

    OAuthProvider provider = oauthProviderRegistry.require(PROVIDER);
    OAuthUserInfo userInfo =
        provider.exchangeAuthorizationCode(
            settings,
            new OAuthTokenExchangeRequest(
                resolvedCode, resolvedCallbackUri, authRequest.getCodeVerifier()));

    String providerUserId = requireValue(userInfo.providerUserId(), "oauth_google_userinfo_failed");
    String email = normalizeEmail(requireValue(userInfo.email(), "oauth_google_userinfo_failed"));
    String name = userInfo.name();

    if (Boolean.FALSE.equals(userInfo.emailVerified())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_email_not_verified");
    }

    EndUser user = resolveOrCreateUser(project, email, name, providerUserId);

    // Consume the state only after we've successfully linked/created the user.
    oauthAuthorizationRequestRepository.delete(authRequest);

    String rawCode = tokenService.generateOAuthExchangeCode();
    OAuthExchangeCode exchange = new OAuthExchangeCode();
    exchange.setProject(project);
    exchange.setUser(user);
    exchange.setProvider(PROVIDER);
    exchange.setCodeHash(tokenService.hashToken(rawCode));
    exchange.setExpiresAt(tokenService.getOAuthExchangeCodeExpiry());
    oauthExchangeCodeRepository.save(exchange);

    return UriComponentsBuilder.fromUriString(authRequest.getAppRedirectUri())
        .queryParam("auctoritas_code", rawCode)
        .build(true)
        .toUriString();
  }

  private EndUser resolveOrCreateUser(
      Project project, String email, String name, String providerUserId) {
    UUID projectId = project.getId();

    Optional<OAuthConnection> existingConn =
        oauthConnectionRepository.findByProjectIdAndProviderAndProviderUserId(
            projectId, PROVIDER, providerUserId);
    if (existingConn.isPresent()) {
      OAuthConnection conn = existingConn.get();
      if (conn.getEmail() == null || !conn.getEmail().equals(email)) {
        conn.setEmail(email);
        oauthConnectionRepository.save(conn);
      }
      return conn.getUser();
    }

    String normalizedName = trimToNull(name);

    EndUser user =
        endUserRepository
            .findByEmailAndProjectIdForUpdate(email, projectId)
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
                  created.setEmail(email);
                  created.setName(normalizedName);
                  created.setEmailVerified(true);
                  // EndUser requires a password hash; OAuth users don't use it.
                  created.setPasswordHash(passwordEncoder.encode(tokenService.generateRefreshToken()));
                  return endUserRepository.save(created);
                });

    OAuthConnection connection = new OAuthConnection();
    connection.setProject(project);
    connection.setUser(user);
    connection.setProvider(PROVIDER);
    connection.setProviderUserId(providerUserId);
    connection.setEmail(email);

    try {
      oauthConnectionRepository.save(connection);
      return user;
    } catch (DataIntegrityViolationException ex) {
      OAuthConnection conn =
          oauthConnectionRepository
              .findByProjectIdAndProviderAndProviderUserId(projectId, PROVIDER, providerUserId)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.CONFLICT, "oauth_connection_conflict", ex));
      if (conn.getEmail() == null || !conn.getEmail().equals(email)) {
        conn.setEmail(email);
        oauthConnectionRepository.save(conn);
      }
      return conn.getUser();
    }
  }


  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private static String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
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
