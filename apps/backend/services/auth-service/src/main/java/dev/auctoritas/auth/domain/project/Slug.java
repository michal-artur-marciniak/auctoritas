package dev.auctoritas.auth.domain.project;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import java.util.Locale;

/**
 * Value object representing a validated project slug.
 * Immutable and enforces slug format at construction.
 */
public record Slug(String value) {
  private static final int MAX_LENGTH = 50;
  private static final String VALID_PATTERN = "^[a-z0-9-]+$";

  /**
   * Creates a Slug from a raw string, normalizing and validating it.
   *
   * @param rawSlug the raw slug string
   * @return validated Slug
   * @throws DomainValidationException if slug is null, empty, or invalid format
   */
  public static Slug of(String rawSlug) {
    if (rawSlug == null || rawSlug.trim().isEmpty()) {
      throw new DomainValidationException("slug_required");
    }

    String normalized = rawSlug.trim().toLowerCase(Locale.ROOT);

    if (normalized.length() > MAX_LENGTH) {
      throw new DomainValidationException("slug_too_long");
    }

    if (!normalized.matches(VALID_PATTERN)) {
      throw new DomainValidationException("slug_invalid_format");
    }

    return new Slug(normalized);
  }

  @Override
  public String toString() {
    return value;
  }
}
