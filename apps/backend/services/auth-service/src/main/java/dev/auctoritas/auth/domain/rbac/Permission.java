package dev.auctoritas.auth.domain.rbac;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Global permission definition used to construct role permission sets.
 */
@Entity
@Table(name = "permissions")
@Getter
public class Permission extends BaseEntity {
  @Column(nullable = false, unique = true, length = 100)
  private String code;

  @Column(nullable = false, length = 255)
  private String description;

  @Column(nullable = false, length = 50)
  private String category;

  protected Permission() {
  }

  /**
   * Seeds a permission with validated values.
   */
  public static Permission seed(PermissionCode code, String description, String category) {
    if (code == null) {
      throw new DomainValidationException("permission_code_required");
    }
    String resolvedDescription = requireValue("permission_description_required", description, 255);
    String resolvedCategory = requireValue("permission_category_required", category, 50);

    Permission permission = new Permission();
    permission.code = code.value();
    permission.description = resolvedDescription;
    permission.category = resolvedCategory;
    return permission;
  }

  private static String requireValue(String errorCode, String value, int maxLength) {
    if (value == null || value.trim().isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }
}
