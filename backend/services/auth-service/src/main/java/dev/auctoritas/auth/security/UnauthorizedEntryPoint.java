package dev.auctoritas.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.auctoritas.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

  private static final String DEFAULT_ERROR_MESSAGE = "Authentication required";
  private static final String AUTH_ERROR_CODE = "AUTH_UNAUTHORIZED";

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) throws IOException {

    log.debug("Unauthorized access attempt to: {} - {}",
        request.getServletPath(), authException.getMessage());

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    ApiResponse<Void> apiResponse = ApiResponse.error(DEFAULT_ERROR_MESSAGE, AUTH_ERROR_CODE);

    objectMapper.writeValue(response.getOutputStream(), apiResponse);
  }
}
