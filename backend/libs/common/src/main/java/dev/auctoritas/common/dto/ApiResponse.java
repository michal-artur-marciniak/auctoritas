package dev.auctoritas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Standard API response wrapper for consistent response format across all services.
 *
 * @param <T> the type of data in the response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    ApiError error,
    Instant timestamp) {

  /**
   * Creates a successful response with data.
   */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, null, Instant.now());
  }

  /**
   * Creates an error response.
   */
  public static <T> ApiResponse<T> error(String code, String message) {
    return new ApiResponse<>(false, null, new ApiError(code, message, null), Instant.now());
  }

  /**
   * Creates an error response with additional details.
   */
  public static <T> ApiResponse<T> error(String code, String message, Map<String, Object> details) {
    return new ApiResponse<>(false, null, new ApiError(code, message, details), Instant.now());
  }

  /**
   * Nested record for error details.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ApiError(String code, String message, Map<String, Object> details) {}
}
