package dev.auctoritas.common.exception;

import dev.auctoritas.common.dto.ApiResponse;
import dev.auctoritas.common.dto.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

  @Mock
  private HttpServletRequest mockRequest;

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
    when(mockRequest.getRequestURI()).thenReturn("/api/v1/test");
  }

  @Nested
  @DisplayName("AuthException Handling")
  class AuthExceptionTests {

    @Test
    @DisplayName("should return 401 with AuthException details")
    void shouldReturn401WithAuthExceptionDetails() {
      AuthException exception = new AuthException("Invalid token", "INVALID_TOKEN", "token123");

      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAuthException(exception, mockRequest);

      assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertFalse(response.getBody().success());
      assertEquals("Invalid token", response.getBody().message());
      assertNotNull(response.getBody().error());
      assertEquals("INVALID_TOKEN", response.getBody().error().code());
      assertEquals("token123", response.getBody().error().field());
    }

    @Test
    @DisplayName("should return 401 with simple AuthException")
    void shouldReturn401WithSimpleAuthException() {
      AuthException exception = new AuthException("Authentication failed");

      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAuthException(exception, mockRequest);

      assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertFalse(response.getBody().success());
      assertEquals("Authentication failed", response.getBody().message());
      assertEquals("AUTH_ERROR", response.getBody().error().code());
    }

    @Test
    @DisplayName("should return 401 with AuthException and cause")
    void shouldReturn401WithAuthExceptionAndCause() {
      AuthException exception = new AuthException("Token expired", "TOKEN_EXPIRED", new RuntimeException("Key expired"));

      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAuthException(exception, mockRequest);

      assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
      assertFalse(response.getBody().success());
      assertEquals("TOKEN_EXPIRED", response.getBody().error().code());
    }
  }

  @Nested
  @DisplayName("Spring AuthenticationException Handling")
  class SpringAuthExceptionTests {

    @Test
    @DisplayName("should return 401 for AuthenticationException")
    void shouldReturn401ForAuthenticationException() {
      AuthenticationException exception = new AuthenticationException("Bad credentials") {};

      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleSpringAuthException(exception, mockRequest);

      assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
      assertFalse(response.getBody().success());
      assertEquals("AUTHENTICATION_FAILED", response.getBody().error().code());
    }

    @Test
    @DisplayName("should return 401 for BadCredentialsException")
    void shouldReturn401ForBadCredentialsException() {
      BadCredentialsException exception = new BadCredentialsException("Invalid username or password");

      ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadCredentialsException(exception, mockRequest);

      assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
      assertFalse(response.getBody().success());
      assertEquals("INVALID_CREDENTIALS", response.getBody().error().code());
    }
  }

  @Nested
  @DisplayName("ValidationException Handling")
  class ValidationExceptionTests {

    @Test
    @DisplayName("should return 400 with validation errors")
    void shouldReturn400WithValidationErrors() {
      List<ErrorDetail> errors = List.of(
          new ErrorDetail("NOT_BLANK", "Email is required", "email"),
          new ErrorDetail("EMAIL", "Invalid email format", "email"),
          new ErrorDetail("SIZE", "Password must be at least 8 characters", "password")
      );
      ValidationException exception = new ValidationException("Validation failed", errors);

      ResponseEntity<ApiResponse<List<ErrorDetail>>> response = exceptionHandler.handleValidationException(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertNotNull(response.getBody());
      assertFalse(response.getBody().success());
      assertEquals(3, response.getBody().data().size());
    }

    @Test
    @DisplayName("should return 400 with single error when errors list is empty")
    void shouldReturn400WithSingleErrorWhenErrorsEmpty() {
      ValidationException exception = new ValidationException("Custom validation message");

      ResponseEntity<ApiResponse<List<ErrorDetail>>> response = exceptionHandler.handleValidationException(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertFalse(response.getBody().success());
      assertEquals(1, response.getBody().data().size());
      assertEquals("VALIDATION_ERROR", response.getBody().data().get(0).code());
    }

    @Test
    @DisplayName("should return 400 with single ErrorDetail")
    void shouldReturn400WithSingleErrorDetail() {
      ErrorDetail error = new ErrorDetail("CUSTOM_ERROR", "Custom error message", "field");
      ValidationException exception = new ValidationException(error);

      ResponseEntity<ApiResponse<List<ErrorDetail>>> response = exceptionHandler.handleValidationException(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals(1, response.getBody().data().size());
      assertEquals("CUSTOM_ERROR", response.getBody().data().get(0).code());
    }
  }

  @Nested
  @DisplayName("MethodArgumentNotValidException Handling")
  class MethodArgumentNotValidExceptionTests {

    @Test
    @DisplayName("should return 400 with field errors from BindingResult")
    void shouldReturn400WithFieldErrorsFromBindingResult() {
      FieldError emailError = new FieldError("dto", "email", "Email is required");
      FieldError passwordError = new FieldError("dto", "password", "Password too short");

      org.springframework.validation.BindingResult bindingResult = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "dto");
      bindingResult.addError(emailError);
      bindingResult.addError(passwordError);

      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

      ResponseEntity<ApiResponse<List<ErrorDetail>>> response =
          exceptionHandler.handleMethodArgumentNotValidException(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertFalse(response.getBody().success());
      assertEquals(2, response.getBody().data().size());
      assertEquals("email", response.getBody().data().get(0).field());
      assertEquals("password", response.getBody().data().get(1).field());
    }

    @Test
    @DisplayName("should handle null default message in FieldError")
    void shouldHandleNullDefaultMessageInFieldError() {
      FieldError fieldError = new FieldError("dto", "field", null, false, null, null, null);

      org.springframework.validation.BindingResult bindingResult = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "dto");
      bindingResult.addError(fieldError);

      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

      ResponseEntity<ApiResponse<List<ErrorDetail>>> response =
          exceptionHandler.handleMethodArgumentNotValidException(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals(1, response.getBody().data().size());
    }
  }

  @Nested
  @DisplayName("ServiceException Handling")
  class ServiceExceptionTests {

    @Test
    @DisplayName("should return 404 for NOT_FOUND error code")
    void shouldReturn404ForNotFoundErrorCode() {
      ServiceException exception = new ServiceException(
          "Organization not found",
          "NOT_FOUND",
          "org-123",
          "Organization"
      );

      ResponseEntity<ApiResponse<ErrorDetail>> response = exceptionHandler.handleServiceException(exception, mockRequest);

      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
      assertFalse(response.getBody().success());
      assertEquals("NOT_FOUND", response.getBody().data().code());
    }

    @Test
    @DisplayName("should return 409 for CONFLICT error code")
    void shouldReturn409ForConflictErrorCode() {
      ServiceException exception = new ServiceException(
          "Email already exists",
          "CONFLICT",
          null,
          null
      );

      ResponseEntity<ApiResponse<ErrorDetail>> response = exceptionHandler.handleServiceException(exception, mockRequest);

      assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("should return 500 for unknown error code")
    void shouldReturn500ForUnknownErrorCode() {
      ServiceException exception = new ServiceException("Unknown error", "UNKNOWN_ERROR");

      ResponseEntity<ApiResponse<ErrorDetail>> response = exceptionHandler.handleServiceException(exception, mockRequest);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @DisplayName("should return 500 for null error code")
    void shouldReturn500ForNullErrorCode() {
      ServiceException exception = new ServiceException("Error without code");

      ResponseEntity<ApiResponse<ErrorDetail>> response = exceptionHandler.handleServiceException(exception, mockRequest);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
  }

  @Nested
  @DisplayName("IllegalArgumentException Handling")
  class IllegalArgumentExceptionTests {

    @Test
    @DisplayName("should return 400 for IllegalArgumentException")
    void shouldReturn400ForIllegalArgumentException() {
      IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter: id");

      ResponseEntity<ApiResponse<ErrorDetail>> response = exceptionHandler.handleIllegalArgumentException(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertFalse(response.getBody().success());
      assertEquals("INVALID_ARGUMENT", response.getBody().data().code());
    }
  }

  @Nested
  @DisplayName("Generic Exception Handling")
  class GenericExceptionTests {

    @Test
    @DisplayName("should return 500 for generic Exception")
    void shouldReturn500ForGenericException() {
      Exception exception = new RuntimeException("Something went wrong");

      ResponseEntity<ApiResponse<ErrorDetail>> response = exceptionHandler.handleGenericException(exception, mockRequest);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
      assertFalse(response.getBody().success());
      assertEquals("INTERNAL_ERROR", response.getBody().data().code());
    }

    @Test
    @DisplayName("should return 500 for Throwable")
    void shouldReturn500ForThrowable() {
      Throwable throwable = new Error("Critical system error");

      ResponseEntity<ApiResponse<ErrorDetail>> response = exceptionHandler.handleThrowable(throwable, mockRequest);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
      assertEquals("CRITICAL_ERROR", response.getBody().data().code());
    }
  }

  @Nested
  @DisplayName("Exception Class Structure Tests")
  class ExceptionStructureTests {

    @Test
    @DisplayName("AuthException should have correct properties")
    void authExceptionShouldHaveCorrectProperties() {
      AuthException exception = new AuthException("message", "CODE", "details", new RuntimeException("cause"));

      assertEquals("message", exception.getMessage());
      assertEquals("CODE", exception.getErrorCode());
      assertEquals("details", exception.getDetails());
      assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("ValidationException should manage errors list correctly")
    void validationExceptionShouldManageErrorsListCorrectly() {
      List<ErrorDetail> errors = List.of(
          new ErrorDetail("E1", "Error 1", "f1"),
          new ErrorDetail("E2", "Error 2", "f2")
      );

      ValidationException exception = new ValidationException("Failed", errors);

      assertTrue(exception.hasErrors());
      assertEquals(2, exception.getErrors().size());
      assertTrue(exception.hasFieldError("f1"));
      assertFalse(exception.hasFieldError("f3"));
      assertNotNull(exception.getFirstErrorForField("f1"));
      assertNull(exception.getFirstErrorForField("f3"));
    }

    @Test
    @DisplayName("ServiceException should have resource info")
    void serviceExceptionShouldHaveResourceInfo() {
      ServiceException exception = new ServiceException(
          "Resource not found",
          "NOT_FOUND",
          "res-123",
          "Resource",
          new RuntimeException("cause")
      );

      assertEquals("NOT_FOUND", exception.getErrorCode());
      assertEquals("res-123", exception.getResourceId());
      assertEquals("Resource", exception.getResourceType());
      assertTrue(exception.hasResourceInfo());
    }

    @Test
    @DisplayName("ServiceException without resource info should return false for hasResourceInfo")
    void serviceExceptionWithoutResourceInfoShouldReturnFalseForHasResourceInfo() {
      ServiceException exception = new ServiceException("Simple error");

      assertFalse(exception.hasResourceInfo());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle ValidationException with null errors list")
    void shouldHandleValidationExceptionWithNullErrorsList() {
      ValidationException exception = new ValidationException("Error", null);

      ResponseEntity<ApiResponse<List<ErrorDetail>>> response = exceptionHandler.handleValidationException(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals(1, response.getBody().data().size());
    }

    @Test
    @DisplayName("should handle empty errors in ValidationException")
    void shouldHandleEmptyErrorsInValidationException() {
      ValidationException exception = new ValidationException("Error", List.of());

      ResponseEntity<ApiResponse<List<ErrorDetail>>> response = exceptionHandler.handleValidationException(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("ErrorDetail should support null field")
    void errorDetailShouldSupportNullField() {
      ErrorDetail errorDetail = new ErrorDetail("CODE", "Message", null);

      assertEquals("CODE", errorDetail.code());
      assertEquals("Message", errorDetail.message());
      assertNull(errorDetail.field());
    }
  }
}
