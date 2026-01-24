package dev.auctoritas.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting filter for login endpoint. Limits requests to 5 attempts per 15 minutes per IP
 * address.
 */
@Component
@Order(2)
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final String LOGIN_PATH = "/api/v1/org/auth/login";
  private static final int MAX_ATTEMPTS = 5;
  private static final long WINDOW_MILLIS = 15 * 60 * 1000; // 15 minutes

  private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    String method = request.getMethod();

    // Only rate limit POST requests to the login endpoint
    if (!"POST".equalsIgnoreCase(method) || !LOGIN_PATH.equals(path)) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = getClientIp(request);
    RateLimitEntry entry = rateLimitMap.compute(clientIp, (key, existing) -> {
      long now = System.currentTimeMillis();
      if (existing == null || now - existing.windowStart > WINDOW_MILLIS) {
        return new RateLimitEntry(now, new AtomicInteger(1));
      }
      existing.attempts.incrementAndGet();
      return existing;
    });

    if (entry.attempts.get() > MAX_ATTEMPTS) {
      long remainingMillis = WINDOW_MILLIS - (System.currentTimeMillis() - entry.windowStart);
      long remainingSeconds = Math.max(1, remainingMillis / 1000);

      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setHeader("Retry-After", String.valueOf(remainingSeconds));
      
      Map<String, Object> errorBody = Map.of(
          "error", "rate_limit_exceeded",
          "message", "Too many login attempts. Please try again later.",
          "retryAfterSeconds", remainingSeconds,
          "timestamp", Instant.now().toString());
      
      objectMapper.writeValue(response.getWriter(), errorBody);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      // Take the first IP in case of multiple proxies
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isBlank()) {
      return xRealIp.trim();
    }
    return request.getRemoteAddr();
  }

  private static class RateLimitEntry {
    final long windowStart;
    final AtomicInteger attempts;

    RateLimitEntry(long windowStart, AtomicInteger attempts) {
      this.windowStart = windowStart;
      this.attempts = attempts;
    }
  }
}
