package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.auctoritas.auth.api.EndUserPasswordForgotRequest;
import dev.auctoritas.auth.api.EndUserPasswordResetResponse;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserPasswordResetToken;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.messaging.DomainEventPublisher;
import dev.auctoritas.auth.messaging.PasswordResetRequestedEvent;
import dev.auctoritas.auth.repository.EndUserPasswordResetTokenRepository;
import dev.auctoritas.common.enums.ApiKeyStatus;
import dev.auctoritas.common.enums.OrganizationStatus;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EndUserPasswordResetServiceTest {

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
  @Autowired private EndUserPasswordResetService passwordResetService;
  @Autowired private EndUserPasswordResetTokenRepository resetTokenRepository;
  @Autowired private TokenService tokenService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private InMemoryDomainEventPublisher domainEventPublisher;

  private static final String RAW_API_KEY = "pk_live_test_password_reset";
  private Project project;
  private EndUser user;

  @BeforeEach
  void setUp() {
    domainEventPublisher.events().clear();

    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-password-reset");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);

    ProjectSettings settings = new ProjectSettings();

    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-password-reset");
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

    user = new EndUser();
    user.setProject(project);
    user.setEmail("user@example.com");
    user.setPasswordHash(passwordEncoder.encode("UserPass123!"));
    entityManager.persist(user);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void requestResetDoesNotLeakTokenAndPublishesEvent() {
    EndUserPasswordResetResponse response =
        passwordResetService.requestReset(
            RAW_API_KEY,
            new EndUserPasswordForgotRequest("user@example.com"),
            "1.2.3.4",
            "test-agent");

    assertThat(response.message()).contains("If an account exists");
    assertThat(response.resetToken()).isNull();

    List<EndUserPasswordResetToken> tokens = resetTokenRepository.findAll();
    assertThat(tokens).hasSize(1);
    EndUserPasswordResetToken token = tokens.getFirst();
    assertThat(token.getUser().getId()).isNotNull();
    assertThat(token.getProject().getId()).isNotNull();
    assertThat(token.getTokenHash()).isNotBlank();
    assertThat(token.getUsedAt()).isNull();
    assertThat(token.getIpAddress()).isEqualTo("1.2.3.4");
    assertThat(token.getUserAgent()).isEqualTo("test-agent");

    assertThat(domainEventPublisher.events()).hasSize(1);
    InMemoryDomainEventPublisher.PublishedEvent published = domainEventPublisher.events().getFirst();
    assertThat(published.type()).isEqualTo(PasswordResetRequestedEvent.EVENT_TYPE);
    assertThat(published.payload()).isInstanceOf(PasswordResetRequestedEvent.class);
    PasswordResetRequestedEvent event = (PasswordResetRequestedEvent) published.payload();
    assertThat(event.projectId()).isEqualTo(project.getId());
    assertThat(event.userId()).isEqualTo(user.getId());
    assertThat(event.email()).isEqualTo("user@example.com");
    assertThat(event.resetToken()).isNotBlank();
    assertThat(event.expiresAt()).isNotNull();

    assertThat(token.getTokenHash()).isEqualTo(tokenService.hashToken(event.resetToken()));
  }

  @Test
  void requestResetInvalidatesExistingTokensForSameUserAndProject() {
    passwordResetService.requestReset(
        RAW_API_KEY, new EndUserPasswordForgotRequest("user@example.com"), null, null);

    entityManager.flush();
    entityManager.clear();

    passwordResetService.requestReset(
        RAW_API_KEY, new EndUserPasswordForgotRequest("user@example.com"), null, null);

    List<EndUserPasswordResetToken> tokens = resetTokenRepository.findAll();
    assertThat(tokens).hasSize(2);
    assertThat(tokens.stream().filter(t -> t.getUsedAt() == null)).hasSize(1);
    assertThat(tokens.stream().filter(t -> t.getUsedAt() != null)).hasSize(1);

    assertThat(domainEventPublisher.events()).hasSize(2);
  }

  @Test
  void requestResetUnknownEmailDoesNotCreateTokenOrPublishEvent() {
    EndUserPasswordResetResponse response =
        passwordResetService.requestReset(
            RAW_API_KEY, new EndUserPasswordForgotRequest("missing@example.com"), null, null);

    assertThat(response.message()).contains("If an account exists");
    assertThat(response.resetToken()).isNull();
    assertThat(resetTokenRepository.findAll()).isEmpty();
    assertThat(domainEventPublisher.events()).isEmpty();
  }
}
