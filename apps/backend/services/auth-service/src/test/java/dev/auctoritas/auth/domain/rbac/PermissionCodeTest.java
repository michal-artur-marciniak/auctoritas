package dev.auctoritas.auth.domain.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

class PermissionCodeTest {

  @Test
  void createsValidPermissionCode() {
    PermissionCode code = PermissionCode.of("users:read");
    assertThat(code.value()).isEqualTo("users:read");
  }

  @Test
  void normalizesToLowercase() {
    PermissionCode code = PermissionCode.of("Users:Read");
    assertThat(code.value()).isEqualTo("users:read");
  }

  @Test
  void rejectsInvalidFormat() {
    assertThatThrownBy(() -> PermissionCode.of("users.read"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("permission_code_invalid_format");
  }
}
