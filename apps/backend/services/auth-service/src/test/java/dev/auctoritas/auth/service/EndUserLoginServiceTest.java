package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserLoginRequest;
import dev.auctoritas.auth.api.EndUserLoginResponse;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.domain.model.enduser.Email;
import dev.auctoritas.auth.domain.model.enduser.Password;
import dev.auctoritas.auth.domain.model.project.Slug;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EndUserLoginServiceTest {
  private static final String RAW_API_KEY = "pk_live_test_login";

  @Autowired private EntityManager entityManager;
  @Autowired private EndUserLoginService loginService;
  @Autowired private TokenService tokenService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  private Project project;

  @BeforeEach
  void setUp() {
    Organization org = Organization.create("Test Org", Slug.of("test-org-login"));
    entityManager.persist(org);

    project = Project.create(org, "Test Project", Slug.of("test-project-login"));
    entityManager.persist(project);

    ApiKey apiKey = ApiKey.create(
        project,
        "Test Key",
        "pk_live_",
        tokenService.hashToken(RAW_API_KEY));
    entityManager.persist(apiKey);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void loginAllowsUnverifiedUserByDefaultAndIncludesEmailVerifiedClaim() {
    Project managedProject = entityManager.find(Project.class, project.getId());
    EndUser user = EndUser.create(
        managedProject,
        Email.of("user@example.com"),
        Password.fromHash(passwordEncoder.encode("UserPass123!")),
        null);
    entityManager.persist(user);
    entityManager.flush();
    entityManager.clear();

    EndUserLoginResponse response =
        loginService.login(
            RAW_API_KEY,
            new EndUserLoginRequest("user@example.com", "UserPass123!"),
            "1.2.3.4",
            "test-agent");

    assertThat(response.user().email()).isEqualTo("user@example.com");
    assertThat(response.user().emailVerified()).isFalse();
    assertThat(response.accessToken()).isNotBlank();

    JwtService.JwtValidationResult validation = jwtService.validateToken(response.accessToken());
    assertThat(validation.valid()).isTrue();
    assertThat(validation.claims().get(JwtService.CLAIM_EMAIL_VERIFIED, Boolean.class)).isEqualTo(false);
  }

  @Test
  void loginRejectsUnverifiedUserWhenProjectRequiresVerifiedEmail() {
    ProjectSettings settings = entityManager.find(Project.class, project.getId()).getSettings();
    settings.updateAuthSettings(true);
    entityManager.flush();

    Project managedProject = entityManager.find(Project.class, project.getId());
    EndUser user = EndUser.create(
        managedProject,
        Email.of("user2@example.com"),
        Password.fromHash(passwordEncoder.encode("UserPass123!")),
        null);
    entityManager.persist(user);
    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(
            () ->
                loginService.login(
                    RAW_API_KEY,
                    new EndUserLoginRequest("user2@example.com", "UserPass123!"),
                    null,
                    null))
        .hasMessageContaining("email_not_verified");
  }

  @Test
  void loginAllowsVerifiedUserWhenProjectRequiresVerifiedEmail() {
    ProjectSettings settings = entityManager.find(Project.class, project.getId()).getSettings();
    settings.updateAuthSettings(true);
    entityManager.flush();

    Project managedProject = entityManager.find(Project.class, project.getId());
    EndUser user = EndUser.create(
        managedProject,
        Email.of("user3@example.com"),
        Password.fromHash(passwordEncoder.encode("UserPass123!")),
        null);
    user.verifyEmail();
    entityManager.persist(user);
    entityManager.flush();
    entityManager.clear();

    EndUserLoginResponse response =
        loginService.login(
            RAW_API_KEY,
            new EndUserLoginRequest("user3@example.com", "UserPass123!"),
            null,
            null);

    assertThat(response.user().email()).isEqualTo("user3@example.com");
    assertThat(response.user().emailVerified()).isTrue();
    JwtService.JwtValidationResult validation = jwtService.validateToken(response.accessToken());
    assertThat(validation.valid()).isTrue();
    assertThat(validation.claims().get(JwtService.CLAIM_EMAIL_VERIFIED, Boolean.class)).isEqualTo(true);
  }
}
