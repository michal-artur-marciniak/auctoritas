package dev.auctoritas.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting filter for login endpoint using Redis. Limits requests to 5 attempts per 15 minutes
 * per IP address.
 */
@Component
@Order(2)
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final String LOGIN_PATH = "/api/v1/org/auth/login";
  private static final String REDIS_KEY_PREFIX = "rate_limit:login:";
  private static final int MAX_ATTEMPTS = 5;
  private static final Duration WINDOW_DURATION = Duration.ofMinutes(15);
  private static final RedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
      "local current = redis.call('INCR', KEYS[1]); "
          + "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
          + "return current;",
      Long.class);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final List<String> trustedProxies;

  public LoginRateLimitFilter(
      StringRedisTemplate redisTemplate,
      @Value("${auth.security.trusted-proxies:}") List<String> trustedProxies) {
    this.redisTemplate = redisTemplate;
    this.trustedProxies = trustedProxies == null
        ? List.of()
        : trustedProxies.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toUnmodifiableList());
  }

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
    String redisKey = REDIS_KEY_PREFIX + clientIp;

    Long attempts = redisTemplate.execute(
        RATE_LIMIT_SCRIPT,
        List.of(redisKey),
        String.valueOf(WINDOW_DURATION.getSeconds()));
    if (attempts == null) {
      attempts = 1L;
    }

    if (attempts > MAX_ATTEMPTS) {
      Long ttl = redisTemplate.getExpire(redisKey);
      long remainingSeconds = ttl != null && ttl > 0 ? ttl : 1;

      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setHeader("Retry-After", String.valueOf(remainingSeconds));

      Map<String, Object> errorBody =
          Map.of(
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
    if (isFromTrustedProxy(request)) {
      String xForwardedFor = request.getHeader("X-Forwarded-For");
      if (xForwardedFor != null && !xForwardedFor.isBlank()) {
        // Take the first IP in case of multiple proxies
        return xForwardedFor.split(",")[0].trim();
      }
      String xRealIp = request.getHeader("X-Real-IP");
      if (xRealIp != null && !xRealIp.isBlank()) {
        return xRealIp.trim();
      }
    }
    return request.getRemoteAddr();
  }

  private boolean isFromTrustedProxy(HttpServletRequest request) {
    if (trustedProxies.isEmpty()) {
      return false;
    }
    return trustedProxies.contains(request.getRemoteAddr());
  }
}
