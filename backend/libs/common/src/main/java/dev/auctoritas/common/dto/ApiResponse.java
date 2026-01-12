package dev.auctoritas.common.dto;

public record ApiResponse<T>(boolean success, String message, T data, ErrorDetail error) {
  // Compact constructor for validation
  public ApiResponse {
    if (success && error != null) {
      throw new IllegalArgumentException("Cannot have error in successful response");
    }
  }

  // Factory methods
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, null, data, null);
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(true, message, data, null);
  }

  public static <T> ApiResponse<T> error(String message, String code) {
    return new ApiResponse<>(false, message, null, new ErrorDetail(code, message));
  }

  public static <T> ApiResponse<T> error(String message, String code, String field) {
    return new ApiResponse<>(false, message, null, new ErrorDetail(code, message, field));
  }
}
