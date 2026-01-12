package dev.auctoritas.common.util;

import java.util.regex.Pattern;

public class ValidationUtils {

  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
  );

  private static final Pattern SLUG_PATTERN = Pattern.compile(
      "^[a-z0-9-]{3,50}$"
  );

  public static boolean isValidEmail(String email) {
    if (email == null || email.length() > 255) {
      return false;
    }
    return EMAIL_PATTERN.matcher(email).matches();
  }

  public static boolean isValidSlug(String slug) {
    if (slug == null) {
      return false;
    }
    return SLUG_PATTERN.matcher(slug).matches();
  }

  public static boolean isValidPassword(String password, int minLength, int maxLength) {
    if (password == null || password.length() < minLength || password.length() > maxLength) {
      return false;
    }
    return true;
  }
}
