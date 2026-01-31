package dev.auctoritas.auth.service.oauth;

import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.oauth.OAuthConnection;
import dev.auctoritas.auth.domain.model.oauth.OAuthAccountLinkingDomainService;
import dev.auctoritas.auth.domain.model.oauth.OAuthAccountLinkingDomainService.EndUserUpdate;
import dev.auctoritas.auth.domain.model.oauth.OAuthAccountLinkingDomainService.LinkingResult;
import dev.auctoritas.auth.domain.model.oauth.OAuthAccountLinkingDomainService.OAuthConnectionUpdate;
import dev.auctoritas.auth.domain.model.oauth.OAuthAccountLinkingDomainService.UserCreationSpec;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.enduser.Email;
import dev.auctoritas.auth.domain.model.enduser.Password;
import dev.auctoritas.auth.domain.model.enduser.EndUserRepositoryPort;
import dev.auctoritas.auth.ports.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.domain.model.oauth.OAuthConnectionRepositoryPort;
import dev.auctoritas.auth.messaging.OAuthUserRegisteredEvent;
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
  private final DomainEventPublisherPort domainEventPublisherPort;
  private final OAuthAccountLinkingDomainService domainService;

  public OAuthAccountLinkingService(
      EndUserRepositoryPort endUserRepository,
      OAuthConnectionRepositoryPort oauthConnectionRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.endUserRepository = endUserRepository;
    this.oauthConnectionRepository = oauthConnectionRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.domainEventPublisherPort = domainEventPublisherPort;
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
      conn.updateEmail(update.newEmail());
      oauthConnectionRepository.save(conn);

      // Publish and clear domain events
      conn.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
      conn.clearDomainEvents();
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
    EndUser user = createOAuthEndUser(project, provider, providerUserId, email, name, isEmailVerified);

    // Create OAuth connection with retry for race conditions
    return createConnectionWithRetry(project.getId(), provider, providerUserId, email, user);
  }

  private EndUser createConnectionWithRetry(
      UUID projectId,
      String provider,
      String providerUserId,
      String email,
      EndUser user) {

    OAuthConnection connection = OAuthConnection.establish(
        user.getProject(), user, provider, providerUserId, email);

    try {
      oauthConnectionRepository.save(connection);

      // Publish and clear domain events
      connection.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
      connection.clearDomainEvents();

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
      conn.updateEmail(email);
      oauthConnectionRepository.save(conn);

      // Publish and clear domain events
      conn.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
      conn.clearDomainEvents();
    }

    return lockUser(projectId, conn.getUser());
  }

  private EndUser createOAuthEndUser(
      Project project,
      String provider,
      String providerUserId,
      String email,
      String name,
      boolean emailVerified) {
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

    EndUser savedUser = endUserRepository.save(user);
    publishUserDomainEventsForOAuthRegistration(savedUser, provider, providerUserId);
    return savedUser;
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
    publishUserDomainEvents(user);
  }

  private EndUser lockUser(UUID projectId, EndUser user) {
    if (user == null || user.getId() == null) {
      throw new DomainNotFoundException("user_not_found");
    }
    return endUserRepository
        .findByIdAndProjectIdForUpdate(user.getId(), projectId)
        .orElseThrow(() -> new DomainNotFoundException("user_not_found"));
  }

  private void publishUserDomainEvents(EndUser user) {
    user.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    user.clearDomainEvents();
  }

  private void publishUserDomainEventsForOAuthRegistration(
      EndUser user,
      String provider,
      String providerUserId) {
    boolean userRegisteredPublished = false;
    for (var event : user.getDomainEvents()) {
      if (event instanceof dev.auctoritas.auth.domain.model.enduser.UserRegisteredEvent) {
        if (!userRegisteredPublished) {
          domainEventPublisherPort.publish(
              OAuthUserRegisteredEvent.EVENT_TYPE,
              new OAuthUserRegisteredEvent(
                  user.getProject().getId(),
                  user.getId(),
                  user.getEmail(),
                  user.getName(),
                  user.isEmailVerified(),
                  provider,
                  providerUserId));
          userRegisteredPublished = true;
        }
        continue;
      }
      domainEventPublisherPort.publish(event.eventType(), event);
    }
    user.clearDomainEvents();
  }
}
