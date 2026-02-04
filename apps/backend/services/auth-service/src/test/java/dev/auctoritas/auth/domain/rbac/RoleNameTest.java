package dev.auctoritas.auth.domain.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

class RoleNameTest {

  @Test
  void createsValidRoleName() {
    RoleName name = RoleName.of("Project Admin");
    assertThat(name.value()).isEqualTo("Project Admin");
  }

  @Test
  void trimsWhitespace() {
    RoleName name = RoleName.of("  admin  ");
    assertThat(name.value()).isEqualTo("admin");
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(() -> RoleName.of("   "))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("role_name_required");
  }

  @Test
  void rejectsInvalidCharacters() {
    assertThatThrownBy(() -> RoleName.of("admin!"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("role_name_invalid_format");
  }
}
