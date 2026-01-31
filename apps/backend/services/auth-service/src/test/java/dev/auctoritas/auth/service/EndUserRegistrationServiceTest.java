package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.auctoritas.auth.application.enduser.EndUserRegistrationCommand;
import dev.auctoritas.auth.application.enduser.EndUserRegistrationResult;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.enduser.EndUserEmailVerificationToken;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.messaging.DomainEventPublisher;
import dev.auctoritas.auth.messaging.UserRegisteredEvent;
import dev.auctoritas.auth.ports.identity.EndUserEmailVerificationTokenRepositoryPort;
import dev.auctoritas.auth.ports.identity.EndUserRepositoryPort;
import dev.auctoritas.auth.domain.apikey.ApiKeyStatus;
import dev.auctoritas.auth.domain.organization.OrganizationStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EndUserRegistrationServiceTest {

  @TestConfiguration
  static class Config {
    @Bean
    @Primary
    InMemoryDomainEventPublisher inMemoryDomainEventPublisher() {
      return new InMemoryDomainEventPublisher();
    }
  }

  static final class InMemoryDomainEventPublisher implements DomainEventPublisher {
    record PublishedEvent(String type, Object payload) {}

    private final List<PublishedEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(String eventType, Object payload) {
      events.add(new PublishedEvent(eventType, payload));
    }

    List<PublishedEvent> events() {
      return events;
    }
  }

  @Autowired private EntityManager entityManager;
  @Autowired private EndUserRegistrationService registrationService;
  @Autowired private EndUserRepositoryPort endUserRepository;
  @Autowired private EndUserEmailVerificationTokenRepositoryPort verificationTokenRepository;
  @Autowired private TokenService tokenService;
  @Autowired private InMemoryDomainEventPublisher domainEventPublisher;

  private static final String RAW_API_KEY = "pk_live_test_registration";
  private Project project;

  @BeforeEach
  void setUp() {
    domainEventPublisher.events().clear();

    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-registration");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);

    ProjectSettings settings = new ProjectSettings();

    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-registration");
    project.setSettings(settings);
    settings.setProject(project);
    entityManager.persist(project);

    ApiKey apiKey = new ApiKey();
    apiKey.setProject(project);
    apiKey.setName("Test Key");
    apiKey.setPrefix("pk_live_");
    apiKey.setKeyHash(tokenService.hashToken(RAW_API_KEY));
    apiKey.setStatus(ApiKeyStatus.ACTIVE);
    entityManager.persist(apiKey);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void registerCreatesEmailVerificationTokenAndPublishesUserRegisteredEvent() {
    EndUserRegistrationResult response =
        registrationService.register(
            RAW_API_KEY,
            new EndUserRegistrationCommand("user@example.com", "UserPass123", "Test User"),
            "1.2.3.4",
            "test-agent");

    assertThat(response.user().email()).isEqualTo("user@example.com");
    assertThat(response.user().emailVerified()).isFalse();
    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();

    List<EndUser> users = entityManager.createQuery("SELECT u FROM EndUser u", EndUser.class).getResultList();
    assertThat(users).hasSize(1);
    EndUser user = users.getFirst();
    assertThat(user.getProject().getId()).isEqualTo(project.getId());
    assertThat(user.getEmail()).isEqualTo("user@example.com");
    assertThat(user.getEmailVerified()).isFalse();

    List<EndUserEmailVerificationToken> tokens = entityManager.createQuery("SELECT t FROM EndUserEmailVerificationToken t", EndUserEmailVerificationToken.class).getResultList();
    assertThat(tokens).hasSize(1);
    EndUserEmailVerificationToken token = tokens.getFirst();
    assertThat(token.getProject().getId()).isEqualTo(project.getId());
    assertThat(token.getUser().getId()).isEqualTo(user.getId());
    assertThat(token.getTokenHash()).isNotBlank();
    assertThat(token.getCodeHash()).isNotBlank();
    assertThat(token.getUsedAt()).isNull();
    assertThat(token.getExpiresAt()).isNotNull();

    assertThat(domainEventPublisher.events()).hasSize(1);
    InMemoryDomainEventPublisher.PublishedEvent published = domainEventPublisher.events().getFirst();
    assertThat(published.type()).isEqualTo(UserRegisteredEvent.EVENT_TYPE);
    assertThat(published.payload()).isInstanceOf(UserRegisteredEvent.class);
    UserRegisteredEvent event = (UserRegisteredEvent) published.payload();
    assertThat(event.projectId()).isEqualTo(project.getId());
    assertThat(event.userId()).isEqualTo(user.getId());
    assertThat(event.email()).isEqualTo("user@example.com");
    assertThat(event.name()).isEqualTo("Test User");
    assertThat(event.emailVerified()).isFalse();
    assertThat(event.emailVerificationTokenId()).isEqualTo(token.getId());
    assertThat(event.emailVerificationExpiresAt()).isEqualTo(token.getExpiresAt());
  }

  @Test
  void registerNormalizesEmail() {
    registrationService.register(
        RAW_API_KEY,
        new EndUserRegistrationCommand("  User@Example.com ", "UserPass123", null),
        null,
        null);

    List<EndUser> users = entityManager.createQuery("SELECT u FROM EndUser u", EndUser.class).getResultList();
    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getEmail()).isEqualTo("user@example.com");

    assertThat(domainEventPublisher.events()).hasSize(1);
    UserRegisteredEvent event = (UserRegisteredEvent) domainEventPublisher.events().getFirst().payload();
    assertThat(event.email()).isEqualTo("user@example.com");
  }
}
