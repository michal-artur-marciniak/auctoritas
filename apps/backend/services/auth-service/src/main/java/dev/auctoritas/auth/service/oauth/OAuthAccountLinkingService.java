package dev.auctoritas.auth.service.oauth;

import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.oauth.OAuthConnection;
import dev.auctoritas.auth.domain.model.oauth.service.OAuthAccountLinkingDomainService;
import dev.auctoritas.auth.domain.model.oauth.service.OAuthAccountLinkingDomainService.EndUserUpdate;
import dev.auctoritas.auth.domain.model.oauth.service.OAuthAccountLinkingDomainService.LinkingResult;
import dev.auctoritas.auth.domain.model.oauth.service.OAuthAccountLinkingDomainService.OAuthConnectionUpdate;
import dev.auctoritas.auth.domain.model.oauth.service.OAuthAccountLinkingDomainService.UserCreationSpec;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.valueobject.Email;
import dev.auctoritas.auth.domain.valueobject.Password;
import dev.auctoritas.auth.ports.identity.EndUserRepositoryPort;
import dev.auctoritas.auth.ports.oauth.OAuthConnectionRepositoryPort;
import dev.auctoritas.auth.service.TokenService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for OAuth account linking.
 *
 * Orchestrates OAuth linking operations by delegating business logic to the domain service
 * and handling infrastructure concerns like repositories, transactions, and locking.
 */
@Service
public class OAuthAccountLinkingService {
  private final EndUserRepositoryPort endUserRepository;
  private final OAuthConnectionRepositoryPort oauthConnectionRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final OAuthAccountLinkingDomainService domainService;

  public OAuthAccountLinkingService(
      EndUserRepositoryPort endUserRepository,
      OAuthConnectionRepositoryPort oauthConnectionRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService) {
    this.endUserRepository = endUserRepository;
    this.oauthConnectionRepository = oauthConnectionRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.domainService = new OAuthAccountLinkingDomainService();
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

    UUID projectId = project.getId();

    // Query existing data
    Optional<OAuthConnection> existingConn =
        oauthConnectionRepository.findByProjectIdAndProviderAndProviderUserId(
            projectId, provider, providerUserId);

    Optional<EndUser> existingVerifiedUser = findVerifiedUserByEmail(projectId, email, emailVerified);

    // Delegate to domain service for decision
    LinkingResult result = domainService.evaluateLinking(
        project,
        provider,
        providerUserId,
        email,
        emailVerified,
        name,
        existingConn,
        existingVerifiedUser);

    // Execute based on result type
    return switch (result.type()) {
      case EXISTING_CONNECTION -> handleExistingConnection(
          projectId, result, provider, providerUserId);
      case EXISTING_USER_LINKED -> handleExistingUserLinked(
          projectId, result, provider, providerUserId, email);
      case NEW_USER_CREATED -> handleNewUserCreated(
          project, provider, providerUserId, email, emailVerified, name);
    };
  }

  private Optional<EndUser> findVerifiedUserByEmail(UUID projectId, String email, Boolean emailVerified) {
    if (!Boolean.TRUE.equals(emailVerified) || email == null) {
      return Optional.empty();
    }
    return endUserRepository.findByEmailAndProjectIdForUpdate(
        email.trim().toLowerCase(), projectId);
  }

  private EndUser handleExistingConnection(
      UUID projectId,
      LinkingResult result,
      String provider,
      String providerUserId) {

    // Update OAuth connection email if needed
    result.connectionUpdate().ifPresent(update -> {
      OAuthConnection conn = update.connection();
      conn.setEmail(update.newEmail());
      oauthConnectionRepository.save(conn);
    });

    // Lock and update user if needed
    EndUser user = lockUser(projectId, result.user());
    result.userUpdate().ifPresent(update -> applyUserUpdate(user, update));
    return user;
  }

  private EndUser handleExistingUserLinked(
      UUID projectId,
      LinkingResult result,
      String provider,
      String providerUserId,
      String email) {

    EndUser user = lockUser(projectId, result.user());

    // Apply user updates
    result.userUpdate().ifPresent(update -> applyUserUpdate(user, update));

    // Create OAuth connection with retry for race conditions
    return createConnectionWithRetry(projectId, provider, providerUserId, email, user);
  }

  private EndUser handleNewUserCreated(
      Project project,
      String provider,
      String providerUserId,
      String email,
      Boolean emailVerified,
      String name) {

    // Handle null emailVerified - default to false
    boolean isEmailVerified = emailVerified != null && emailVerified;

    // Create new user
    EndUser user = createOAuthEndUser(project, email, name, isEmailVerified);

    // Create OAuth connection with retry for race conditions
    return createConnectionWithRetry(project.getId(), provider, providerUserId, email, user);
  }

  private EndUser createConnectionWithRetry(
      UUID projectId,
      String provider,
      String providerUserId,
      String email,
      EndUser user) {

    OAuthConnection connection = new OAuthConnection();
    connection.setProject(user.getProject());
    connection.setUser(user);
    connection.setProvider(provider);
    connection.setProviderUserId(providerUserId);
    connection.setEmail(email);

    try {
      oauthConnectionRepository.save(connection);
      return user;
    } catch (DataIntegrityViolationException ex) {
      // Race condition: another request created the connection
      return handleDuplicateConnection(projectId, provider, providerUserId, email, user);
    }
  }

  private EndUser handleDuplicateConnection(
      UUID projectId,
      String provider,
      String providerUserId,
      String email,
      EndUser user) {

    OAuthConnection conn =
        oauthConnectionRepository
            .findByProjectIdAndProviderAndProviderUserId(projectId, provider, providerUserId)
            .orElseThrow(() -> new DomainConflictException("oauth_connection_conflict"));

    if (!email.equals(conn.getEmail())) {
      conn.setEmail(email);
      oauthConnectionRepository.save(conn);
    }

    return lockUser(projectId, conn.getUser());
  }

  private EndUser createOAuthEndUser(Project project, String email, String name, boolean emailVerified) {
    UserCreationSpec spec = domainService.createUserSpec(project, email, name, emailVerified);

    String randomPassword = tokenService.generateRefreshToken();
    String hashedPassword = passwordEncoder.encode(randomPassword);

    EndUser user = EndUser.create(
        spec.project(),
        spec.email(),
        Password.fromHash(hashedPassword),
        spec.name());

    if (spec.emailVerified()) {
      user.verifyEmail();
    }

    return endUserRepository.save(user);
  }

  private void applyUserUpdate(EndUser user, EndUserUpdate update) {
    if (update.verifyEmail() && !user.isEmailVerified()) {
      user.verifyEmail();
    }

    update.newName().ifPresent(name -> {
      if (user.getName() == null && name != null) {
        user.updateName(name);
      }
    });

    endUserRepository.save(user);
  }

  private EndUser lockUser(UUID projectId, EndUser user) {
    if (user == null || user.getId() == null) {
      throw new DomainNotFoundException("user_not_found");
    }
    return endUserRepository
        .findByIdAndProjectIdForUpdate(user.getId(), projectId)
        .orElseThrow(() -> new DomainNotFoundException("user_not_found"));
  }
}
