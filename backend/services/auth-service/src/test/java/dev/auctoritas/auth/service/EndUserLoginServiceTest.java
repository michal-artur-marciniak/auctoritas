package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserLoginRequest;
import dev.auctoritas.auth.api.EndUserLoginResponse;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.common.enums.ApiKeyStatus;
import dev.auctoritas.common.enums.OrganizationStatus;
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
    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-login");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);

    ProjectSettings settings = new ProjectSettings();

    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-login");
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
  void loginAllowsUnverifiedUserByDefaultAndIncludesEmailVerifiedClaim() {
    EndUser user = new EndUser();
    user.setProject(entityManager.find(Project.class, project.getId()));
    user.setEmail("user@example.com");
    user.setPasswordHash(passwordEncoder.encode("UserPass123!"));
    user.setEmailVerified(false);
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
    settings.setRequireVerifiedEmailForLogin(true);
    entityManager.flush();

    EndUser user = new EndUser();
    user.setProject(entityManager.find(Project.class, project.getId()));
    user.setEmail("user2@example.com");
    user.setPasswordHash(passwordEncoder.encode("UserPass123!"));
    user.setEmailVerified(false);
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
    settings.setRequireVerifiedEmailForLogin(true);
    entityManager.flush();

    EndUser user = new EndUser();
    user.setProject(entityManager.find(Project.class, project.getId()));
    user.setEmail("user3@example.com");
    user.setPasswordHash(passwordEncoder.encode("UserPass123!"));
    user.setEmailVerified(true);
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
