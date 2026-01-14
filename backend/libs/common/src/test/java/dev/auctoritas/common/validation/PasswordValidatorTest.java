package dev.auctoritas.common.validation;

import dev.auctoritas.common.config.PasswordConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

  private PasswordValidator validator;
  private PasswordConfig config;

  @BeforeEach
  void setUp() {
    config = new PasswordConfig();
    config.setPolicy(new PasswordConfig.Policy());
    validator = new PasswordValidator(config);
  }

  @Nested
  @DisplayName("Valid Password Scenarios")
  class ValidPasswordTests {

    @Test
    @DisplayName("should accept valid password meeting all requirements")
    void shouldAcceptValidPassword() {
      ValidationResult result = validator.validate("SecurePass123!");
      assertTrue(result.valid());
      assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("should accept password at minimum length")
    void shouldAcceptPasswordAtMinimumLength() {
      ValidationResult result = validator.validate("Pass123!");
      assertTrue(result.valid());
    }

    @Test
    @DisplayName("should accept password with various special characters")
    void shouldAcceptPasswordWithVariousSpecialChars() {
      ValidationResult result = validator.validate("MyP@ss#123$");
      assertTrue(result.valid());
    }

    @Test
    @DisplayName("should accept password with multiple special characters")
    void shouldAcceptPasswordWithMultipleSpecialChars() {
      ValidationResult result = validator.validate("Str0ng!@#Pass$%^");
      assertTrue(result.valid());
    }

    @Test
    @DisplayName("should handle very long valid password")
    void shouldHandleVeryLongValidPassword() {
      String password = "Aa1!Xy9#Mz2@Qw4$Er6%Tt8^";
      int repetitions = 3;
      String longPart = "bC3dE5fG7hJ9kL1mN3pQ5rS7uV";
      String passwordBase = password + longPart.repeat(repetitions);
      assertTrue(passwordBase.length() > 100 && passwordBase.length() <= 128);
      ValidationResult result = validator.validate(passwordBase);
      assertTrue(result.valid());
    }
  }

  @Nested
  @DisplayName("Length Validation")
  class LengthValidationTests {

    @Test
    @DisplayName("should reject null password")
    void shouldRejectNullPassword() {
      ValidationResult result = validator.validate(null);
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.TOO_SHORT));
    }

    @Test
    @DisplayName("should reject password shorter than minimum length")
    void shouldRejectPasswordTooShort() {
      ValidationResult result = validator.validate("Ab1!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.TOO_SHORT));
    }

    @Test
    @DisplayName("should reject password longer than maximum length")
    void shouldRejectPasswordTooLong() {
      String password = "Aa1!" + "a".repeat(130);
      ValidationResult result = validator.validate(password);
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.TOO_LONG));
    }

    @Test
    @DisplayName("should reject empty password")
    void shouldRejectEmptyPassword() {
      ValidationResult result = validator.validate("");
      assertFalse(result.valid());
    }

    @Test
    @DisplayName("should return correct error messages for length")
    void shouldReturnCorrectErrorMessagesForLength() {
      ValidationResult result = validator.validate("abc");
      assertTrue(result.getErrorMessages().size() >= 1);
      assertTrue(result.getErrorMessages().get(0).toLowerCase().contains("at least"));
    }
  }

  @Nested
  @DisplayName("Character Requirement Validation")
  class CharacterRequirementTests {

    @Test
    @DisplayName("should reject password without uppercase")
    void shouldRejectPasswordWithoutUppercase() {
      ValidationResult result = validator.validate("password123!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.MISSING_UPPERCASE));
    }

    @Test
    @DisplayName("should reject password without lowercase")
    void shouldRejectPasswordWithoutLowercase() {
      ValidationResult result = validator.validate("PASSWORD123!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.MISSING_LOWERCASE));
    }

    @Test
    @DisplayName("should reject password without digit")
    void shouldRejectPasswordWithoutDigit() {
      ValidationResult result = validator.validate("Password!!!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.MISSING_DIGIT));
    }

    @Test
    @DisplayName("should reject password without special character")
    void shouldRejectPasswordWithoutSpecialChar() {
      ValidationResult result = validator.validate("Password123");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.MISSING_SPECIAL_CHAR));
    }

    @Test
    @DisplayName("should report multiple missing requirements")
    void shouldReportMultipleMissingRequirements() {
      ValidationResult result = validator.validateSimple("password");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.MISSING_UPPERCASE));
      assertTrue(result.hasError(ValidationError.MISSING_DIGIT));
      assertTrue(result.hasError(ValidationError.MISSING_SPECIAL_CHAR));
      assertFalse(result.hasError(ValidationError.TOO_SHORT)); // 8 chars = min length
    }
  }

  @Nested
  @DisplayName("Common Password Detection")
  class CommonPasswordTests {

    @Test
    @DisplayName("should reject password 'password'")
    void shouldRejectPasswordCommon() {
      ValidationResult result = validator.validate("password");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.COMMON_PASSWORD));
    }

    @Test
    @DisplayName("should reject password 'password123'")
    void shouldRejectPassword123() {
      ValidationResult result = validator.validate("password123");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.COMMON_PASSWORD));
    }

    @Test
    @DisplayName("should reject 'qwerty123'")
    void shouldRejectQwerty123() {
      ValidationResult result = validator.validate("qwerty123");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.COMMON_PASSWORD));
    }

    @Test
    @DisplayName("should reject 'admin123'")
    void shouldRejectAdmin123() {
      ValidationResult result = validator.validate("admin123");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.COMMON_PASSWORD));
    }

    @Test
    @DisplayName("should not reject unique password")
    void shouldNotRejectUniquePassword() {
      ValidationResult result = validator.validate("Xy7!mP2@qR5#");
      assertFalse(result.hasError(ValidationError.COMMON_PASSWORD));
    }
  }

  @Nested
  @DisplayName("Sequential Character Detection")
  class SequentialCharacterTests {

    @Test
    @DisplayName("should reject password with 'abcd' sequence")
    void shouldRejectAbcdSequence() {
      ValidationResult result = validator.validate("Abcdefg1!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.SEQUENTIAL_CHARS));
    }

    @Test
    @DisplayName("should reject password with '1234' sequence")
    void shouldReject1234Sequence() {
      ValidationResult result = validator.validate("Pass12345!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.SEQUENTIAL_CHARS));
    }

    @Test
    @DisplayName("should reject password with reverse 'wxyz' sequence")
    void shouldRejectReverseSequence() {
      ValidationResult result = validator.validate("Azyxw123!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.SEQUENTIAL_CHARS));
    }

    @Test
    @DisplayName("should accept password without sequences")
    void shouldAcceptWithoutSequences() {
      ValidationResult result = validator.validate("Xk9#mP2@qR5!");
      assertTrue(result.valid());
      assertFalse(result.hasError(ValidationError.SEQUENTIAL_CHARS));
    }

    @Test
    @DisplayName("should accept password with 3-character sequence")
    void shouldAcceptWith3CharSequence() {
      ValidationResult result = validator.validate("Abcefg1!");
      assertFalse(result.hasError(ValidationError.SEQUENTIAL_CHARS));
    }

    @Test
    @DisplayName("should accept password with 123 pattern")
    void shouldAcceptWith123Pattern() {
      ValidationResult result = validator.validate("Secure123!");
      assertFalse(result.hasError(ValidationError.SEQUENTIAL_CHARS));
    }
  }

  @Nested
  @DisplayName("Repeated Character Detection")
  class RepeatedCharacterTests {

    @Test
    @DisplayName("should reject password with 'aaaaa' repeated")
    void shouldRejectAaaaaRepeated() {
      ValidationResult result = validator.validate("Aaaaaa1111!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.REPEATED_CHARS));
    }

    @Test
    @DisplayName("should reject password with '11111' repeated")
    void shouldReject11111Repeated() {
      ValidationResult result = validator.validate("Pass11111!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.REPEATED_CHARS));
    }

    @Test
    @DisplayName("should accept password without excessive repeats")
    void shouldAcceptWithoutExcessiveRepeats() {
      ValidationResult result = validator.validate("SecureP@ss1!");
      assertTrue(result.valid());
      assertFalse(result.hasError(ValidationError.REPEATED_CHARS));
    }

    @Test
    @DisplayName("should accept password with single repeated character")
    void shouldAcceptSingleRepeated() {
      ValidationResult result = validator.validate("Seccure1!");
      assertTrue(result.valid());
      assertFalse(result.hasError(ValidationError.REPEATED_CHARS));
    }

    @Test
    @DisplayName("should accept password with 4 repeated characters")
    void shouldAccept4Repeated() {
      ValidationResult result = validator.validate("Aaaa1111!");
      assertTrue(result.valid());
      assertFalse(result.hasError(ValidationError.REPEATED_CHARS));
    }
  }

  @Nested
  @DisplayName("Whitespace Detection")
  class WhitespaceTests {

    @Test
    @DisplayName("should reject password with leading space")
    void shouldRejectLeadingSpace() {
      ValidationResult result = validator.validate(" Secure1!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.WHITESPACE));
    }

    @Test
    @DisplayName("should reject password with trailing space")
    void shouldRejectTrailingSpace() {
      ValidationResult result = validator.validate("Secure1! ");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.WHITESPACE));
    }

    @Test
    @DisplayName("should reject password with middle space")
    void shouldRejectMiddleSpace() {
      ValidationResult result = validator.validate("Secure 1!");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.WHITESPACE));
    }

    @Test
    @DisplayName("should accept password without whitespace")
    void shouldAcceptWithoutWhitespace() {
      ValidationResult result = validator.validate("Secure1!");
      assertTrue(result.valid());
      assertFalse(result.hasError(ValidationError.WHITESPACE));
    }
  }

  @Nested
  @DisplayName("Validation Result Tests")
  class ValidationResultTests {

    @Test
    @DisplayName("should merge results correctly")
    void shouldMergeResultsCorrectly() {
      ValidationResult result1 = ValidationResult.failure(ValidationError.TOO_SHORT);
      ValidationResult result2 = ValidationResult.failure(ValidationError.MISSING_UPPERCASE);

      ValidationResult merged = result1.merge(result2);

      assertFalse(merged.valid());
      assertEquals(2, merged.errors().size());
    }

    @Test
    @DisplayName("should return valid when merging with success")
    void shouldReturnValidWhenMergingWithSuccess() {
      ValidationResult result1 = ValidationResult.failure(ValidationError.TOO_SHORT);
      ValidationResult result2 = ValidationResult.success();

      ValidationResult merged = result1.merge(result2);

      assertFalse(merged.valid());
      assertEquals(1, merged.errors().size());
    }

    @Test
    @DisplayName("success result should be valid")
    void successResultShouldBeValid() {
      ValidationResult result = ValidationResult.success();
      assertTrue(result.valid());
      assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("failure result should not be valid")
    void failureResultShouldNotBeValid() {
      ValidationResult result = ValidationResult.failure(ValidationError.TOO_SHORT);
      assertFalse(result.valid());
      assertFalse(result.errors().isEmpty());
    }

    @Test
    @DisplayName("getErrorMessages should return formatted messages")
    void getErrorMessagesShouldReturnFormattedMessages() {
      ValidationResult result = ValidationResult.failure(
          ValidationError.TOO_SHORT,
          ValidationError.MISSING_UPPERCASE
      );

      assertEquals(2, result.getErrorMessages().size());
      assertTrue(result.getErrorMessages().get(0).toLowerCase().contains("at least") ||
                  result.getErrorMessages().get(0).toLowerCase().contains("uppercase"));
      assertTrue(result.getErrorMessages().get(1).toLowerCase().contains("at least") ||
                  result.getErrorMessages().get(1).toLowerCase().contains("uppercase"));
    }
  }

  @Nested
  @DisplayName("Simple Validation Tests")
  class SimpleValidationTests {

    @Test
    @DisplayName("simple validate should pass for valid password")
    void simpleValidateShouldPassForValidPassword() {
      ValidationResult result = validator.validateSimple("SecurePass123!");
      assertTrue(result.valid());
    }

    @Test
    @DisplayName("simple validate should fail for common password")
    void simpleValidateShouldFailForCommonPassword() {
      ValidationResult result = validator.validateSimple("password123");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.COMMON_PASSWORD));
    }

    @Test
    @DisplayName("simple validate should not check for sequences")
    void simpleValidateShouldNotCheckSequences() {
      ValidationResult result = validator.validateSimple("Abcdefg1!");
      assertFalse(result.hasError(ValidationError.SEQUENTIAL_CHARS));
    }

    @Test
    @DisplayName("simple validate should not check for repeats")
    void simpleValidateShouldNotCheckRepeats() {
      ValidationResult result = validator.validateSimple("Aaaa1111!");
      assertFalse(result.hasError(ValidationError.REPEATED_CHARS));
    }
  }

  @Nested
  @DisplayName("Configuration Access Tests")
  class ConfigurationAccessTests {

    @Test
    @DisplayName("getMinLength should return configured value")
    void getMinLengthShouldReturnConfiguredValue() {
      assertEquals(8, validator.getMinLength());
    }

    @Test
    @DisplayName("getMaxLength should return configured value")
    void getMaxLengthShouldReturnConfiguredValue() {
      assertEquals(128, validator.getMaxLength());
    }

    @Test
    @DisplayName("getSpecialChars should return configured value")
    void getSpecialCharsShouldReturnConfiguredValue() {
      assertNotNull(validator.getSpecialChars());
      assertTrue(validator.getSpecialChars().contains("!"));
      assertTrue(validator.getSpecialChars().contains("@"));
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle only special characters")
    void shouldHandleOnlySpecialCharacters() {
      ValidationResult result = validator.validate("!@#$%^&*");
      assertFalse(result.valid());
      assertFalse(result.hasError(ValidationError.TOO_SHORT)); // 8 chars = min length
      assertTrue(result.hasError(ValidationError.MISSING_UPPERCASE));
      assertTrue(result.hasError(ValidationError.MISSING_LOWERCASE));
      assertTrue(result.hasError(ValidationError.MISSING_DIGIT));
    }

    @Test
    @DisplayName("should handle only numbers")
    void shouldHandleOnlyNumbers() {
      ValidationResult result = validator.validate("12345678");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.MISSING_UPPERCASE));
      assertTrue(result.hasError(ValidationError.MISSING_LOWERCASE));
      assertTrue(result.hasError(ValidationError.MISSING_SPECIAL_CHAR));
    }

    @Test
    @DisplayName("should handle only letters")
    void shouldHandleOnlyLetters() {
      ValidationResult result = validator.validate("Password");
      assertFalse(result.valid());
      assertTrue(result.hasError(ValidationError.MISSING_DIGIT));
      assertTrue(result.hasError(ValidationError.MISSING_SPECIAL_CHAR));
    }

    @Test
    @DisplayName("should handle Unicode characters")
    void shouldHandleUnicodeCharacters() {
      ValidationResult result = validator.validate("Pässwörd123!");
      assertTrue(result.valid());
    }

    @Test
    @DisplayName("should handle very long valid password at edge")
    void shouldHandleVeryLongValidPasswordAtEdge() {
      String password = "Aa1!Xy9#Mz2@Qw4$Er6%Tt8^Yy0*Ui1!";
      int repetitions = 3;
      String longPart = "bC3dE5fG7hJ9kL1mN3pQ5rS7uV9xZ1";
      String passwordBase = password + longPart.repeat(repetitions);
      assertTrue(passwordBase.length() > 100);
      ValidationResult result = validator.validate(passwordBase);
      assertTrue(result.valid(), "Expected valid but got: " + result.getErrorMessages());
    }
  }
}
