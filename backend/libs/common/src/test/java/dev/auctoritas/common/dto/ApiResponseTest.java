package dev.auctoritas.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApiResponseTest {
  @Test
  void successResponse() {
    ApiResponse<String> response = ApiResponse.success("test");
    assertTrue(response.success());
    assertEquals("test", response.data());
    assertNull(response.error());
  }

  @Test
  void errorResponse() {
    ApiResponse<Void> response = ApiResponse.error("Bad request", "BAD_REQUEST");
    assertFalse(response.success());
    assertEquals("Bad request", response.message());
    assertNotNull(response.error());
  }

  @Test
  void successWithMessage() {
    ApiResponse<String> response = ApiResponse.success("Created", "data");
    assertEquals("Created", response.message());
  }
}
