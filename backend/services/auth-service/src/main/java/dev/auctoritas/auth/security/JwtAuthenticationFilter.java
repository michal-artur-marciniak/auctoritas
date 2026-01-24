package dev.auctoritas.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.auctoritas.auth.service.JwtService;
import dev.auctoritas.auth.service.JwtService.JwtValidationResult;
import dev.auctoritas.common.enums.OrgMemberRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that extracts and validates JWT from Authorization header. Sets the OrgMemberPrincipal in
 * SecurityContext on successful validation.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      // No token present - let Spring Security handle unauthorized access
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(BEARER_PREFIX.length());
    JwtValidationResult result = jwtService.validateToken(token);

    if (!result.valid()) {
      writeErrorResponse(response, result.errorCode());
      return;
    }

    Claims claims = result.claims();
    OrgMemberPrincipal principal = extractPrincipal(claims);
    SecurityContextHolder.getContext().setAuthentication(principal);

    filterChain.doFilter(request, response);
  }

  private OrgMemberPrincipal extractPrincipal(Claims claims) {
    UUID orgMemberId = UUID.fromString(claims.get(JwtService.CLAIM_ORG_MEMBER_ID, String.class));
    UUID orgId = UUID.fromString(claims.get(JwtService.CLAIM_ORG_ID, String.class));
    String email = claims.get(JwtService.CLAIM_EMAIL, String.class);
    OrgMemberRole role = OrgMemberRole.valueOf(claims.get(JwtService.CLAIM_ROLE, String.class));

    return new OrgMemberPrincipal(orgMemberId, orgId, email, role);
  }

  private void writeErrorResponse(HttpServletResponse response, String errorCode)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), Map.of("error", errorCode));
  }
}
