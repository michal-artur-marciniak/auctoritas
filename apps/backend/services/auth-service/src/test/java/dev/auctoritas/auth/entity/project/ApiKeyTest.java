package dev.auctoritas.auth.domain.model.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.domain.apikey.ApiKeyStatus;
import org.junit.jupiter.api.Test;

class ApiKeyTest {

  @Test
  void createSetsFieldsAndDefaults() {
    Project project = new Project();

    ApiKey apiKey = ApiKey.create(project, "Primary", "pk_live_", "hash");

    assertThat(apiKey.getProject()).isSameAs(project);
    assertThat(apiKey.getName()).isEqualTo("Primary");
    assertThat(apiKey.getPrefix()).isEqualTo("pk_live_");
    assertThat(apiKey.getKeyHash()).isEqualTo("hash");
    assertThat(apiKey.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
  }

  @Test
  void createRequiresProject() {
    assertThatThrownBy(() -> ApiKey.create(null, "Primary", "pk_live_", "hash"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createRequiresName() {
    Project project = new Project();

    assertThatThrownBy(() -> ApiKey.create(project, null, "pk_live_", "hash"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ApiKey.create(project, "   ", "pk_live_", "hash"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createRequiresPrefix() {
    Project project = new Project();

    assertThatThrownBy(() -> ApiKey.create(project, "Primary", null, "hash"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ApiKey.create(project, "Primary", "   ", "hash"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createRequiresKeyHash() {
    Project project = new Project();

    assertThatThrownBy(() -> ApiKey.create(project, "Primary", "pk_live_", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ApiKey.create(project, "Primary", "pk_live_", "   "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createRejectsOversizedValues() {
    Project project = new Project();

    assertThatThrownBy(() -> ApiKey.create(project, "a".repeat(51), "pk_live_", "hash"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ApiKey.create(project, "Primary", "p".repeat(11), "hash"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ApiKey.create(project, "Primary", "pk_live_", "h".repeat(65)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
