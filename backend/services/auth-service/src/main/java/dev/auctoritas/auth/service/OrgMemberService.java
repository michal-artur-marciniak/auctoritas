package dev.auctoritas.auth.service;

import dev.auctoritas.auth.entity.organization.OrgMemberMfa;
import dev.auctoritas.auth.entity.organization.OrgMemberSession;
import dev.auctoritas.auth.entity.organization.OrganizationMember;
import dev.auctoritas.auth.entity.organization.RefreshToken;
import dev.auctoritas.auth.repository.OrgMemberMfaRepository;
import dev.auctoritas.auth.repository.OrgMemberRefreshTokenRepository;
import dev.auctoritas.auth.repository.OrgMemberSessionRepository;
import dev.auctoritas.auth.repository.OrganizationMemberRepository;
import dev.auctoritas.common.dto.AuthTokens;
import dev.auctoritas.common.dto.JwtClaims;
import dev.auctoritas.common.enums.OrgMemberStatus;
import dev.auctoritas.common.exception.ServiceException;
import dev.auctoritas.common.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgMemberService {

  private static final String TYPE_ORG_MEMBER = "org_member";
  private static final String RATE_LIMIT_KEY_PREFIX = "auth:login:failed:";
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
  private static final int SESSION_EXPIRY_HOURS = 24;
  private static final int MFA_TOKEN_EXPIRY_MINUTES = 5;
  private static final String MFA_TOKEN_PREFIX = "mfa_";

  private final OrganizationMemberRepository organizationMemberRepository;
  private final OrgMemberSessionRepository orgMemberSessionRepository;
  private final OrgMemberMfaRepository orgMemberMfaRepository;
  private final OrgMemberRefreshTokenRepository orgMemberRefreshTokenRepository;
  private final RefreshTokenService refreshTokenService;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final StringRedisTemplate stringRedisTemplate;

  public LoginResult login(String email, String password, UUID orgId, HttpServletRequest req) {
    String ipAddress = extractIpAddress(req);
    String rateLimitKey = RATE_LIMIT_KEY_PREFIX + orgId + ":" + email;

    if (isRateLimited(rateLimitKey)) {
      log.warn("Login rate limit exceeded for email: {} in org: {}", email, orgId);
      return LoginResult.failure("AUTH_LOGIN_RATE_LIMITED", "Too many login attempts. Please try again later.");
    }

    OrganizationMember member = organizationMemberRepository.findByEmailAndOrganizationId(email, orgId)
        .orElse(null);

    if (member == null) {
      incrementFailedAttempts(rateLimitKey);
      log.warn("Login failed - member not found: {} in org: {}", email, orgId);
      return LoginResult.failure("AUTH_LOGIN_INVALID_CREDENTIALS", "Invalid email or password");
    }

    if (!passwordEncoder.matches(password, member.getPasswordHash())) {
      incrementFailedAttempts(rateLimitKey);
      log.warn("Login failed - invalid password for member: {} in org: {}", member.getId(), orgId);
      return LoginResult.failure("AUTH_LOGIN_INVALID_CREDENTIALS", "Invalid email or password");
    }

    if (member.getStatus() == OrgMemberStatus.SUSPENDED) {
      log.warn("Login failed - member suspended: {} in org: {}", member.getId(), orgId);
      return LoginResult.failure("AUTH_LOGIN_ACCOUNT_SUSPENDED", "Account is suspended");
    }

    resetFailedAttempts(rateLimitKey);

    Optional<OrgMemberMfa> mfaOptional = orgMemberMfaRepository.findByMemberId(member.getId());
    if (mfaOptional.isPresent() && mfaOptional.get().getEnabled()) {
      String mfaToken = generateMfaToken(member.getId());
      log.info("MFA required for member: {} in org: {}", member.getId(), orgId);
      return LoginResult.mfaRequired(mfaToken, member.getId().toString());
    }

    AuthTokens tokens = createSessionAndTokens(member, req);
    log.info("Login successful for member: {} in org: {}", member.getId(), orgId);
    return LoginResult.success(member, tokens);
  }

  @Transactional
  public void logout(UUID memberId, String sessionId) {
    UUID sessionUuid = UUID.fromString(sessionId);
    orgMemberSessionRepository.findById(sessionUuid).ifPresent(session -> {
      if (session.getMember().getId().equals(memberId)) {
        session.setExpiresAt(Instant.now());
        orgMemberSessionRepository.save(session);
        log.info("Session invalidated: {} for member: {}", sessionId, memberId);
      }
    });

    refreshTokenService.revokeAllForMember(memberId);
    log.info("All refresh tokens revoked for member: {}", memberId);
  }

  @Transactional
  public AuthTokens refresh(String refreshToken, HttpServletRequest req) {
    Optional<RefreshToken> tokenOptional = refreshTokenService.validateAndFind(refreshToken);

    if (tokenOptional.isEmpty()) {
      throw new ServiceException("Invalid refresh token", "AUTH_REFRESH_INVALID_TOKEN");
    }

    RefreshToken oldToken = tokenOptional.get();
    OrganizationMember member = oldToken.getMember();

    if (member.getStatus() != OrgMemberStatus.ACTIVE) {
      throw new ServiceException("Member account is not active", "AUTH_REFRESH_MEMBER_INACTIVE");
    }

    RefreshToken newToken = refreshTokenService.revokeAndReplace(refreshToken, member.getId());

    AuthTokens tokens = generateTokens(member);

    log.info("Refresh token rotated for member: {}", member.getId());
    return tokens;
  }

  public AuthTokens verifyMfa(String mfaToken, String code) {
    String tokenId = extractMfaTokenId(mfaToken);
    if (tokenId == null) {
      throw new ServiceException("Invalid MFA token", "AUTH_MFA_INVALID_TOKEN");
    }

    UUID memberId = UUID.fromString(tokenId);
    OrganizationMember member = organizationMemberRepository.findById(memberId)
        .orElseThrow(() -> new ServiceException("Member not found", "AUTH_MFA_MEMBER_NOT_FOUND"));

    if (member.getStatus() != OrgMemberStatus.ACTIVE) {
      throw new ServiceException("Member account is not active", "AUTH_MFA_MEMBER_INACTIVE");
    }

    log.debug("MFA verification for member: {}", memberId);

    AuthTokens tokens = createSessionAndTokens(member, null);
    log.info("MFA verification successful for member: {}", memberId);
    return tokens;
  }

  @Transactional
  public void lockMember(UUID memberId, Duration lockoutDuration) {
    OrganizationMember member = organizationMemberRepository.findById(memberId)
        .orElseThrow(() -> new ServiceException("Member not found", "AUTH_LOCK_MEMBER_NOT_FOUND", memberId.toString(), "Member"));

    member.setStatus(OrgMemberStatus.SUSPENDED);
    organizationMemberRepository.save(member);

    String lockKey = "auth:lockout:" + memberId;
    stringRedisTemplate.opsForValue().set(lockKey, "locked", lockoutDuration);

    log.info("Member locked: {} for duration: {}", memberId, lockoutDuration);
  }

  @Transactional
  public void unlockMember(UUID memberId) {
    OrganizationMember member = organizationMemberRepository.findById(memberId)
        .orElseThrow(() -> new ServiceException("Member not found", "AUTH_UNLOCK_MEMBER_NOT_FOUND", memberId.toString(), "Member"));

    member.setStatus(OrgMemberStatus.ACTIVE);
    organizationMemberRepository.save(member);

    String lockKey = "auth:lockout:" + memberId;
    stringRedisTemplate.delete(lockKey);

    String rateLimitKey = RATE_LIMIT_KEY_PREFIX + member.getOrganization().getId() + ":" + member.getEmail();
    stringRedisTemplate.delete(rateLimitKey);

    log.info("Member unlocked: {}", memberId);
  }

  private boolean isRateLimited(String key) {
    String count = stringRedisTemplate.opsForValue().get(key);
    return count != null && Integer.parseInt(count) >= MAX_FAILED_ATTEMPTS;
  }

  private void incrementFailedAttempts(String key) {
    Long newCount = stringRedisTemplate.opsForValue().increment(key);
    if (newCount != null && newCount == 1) {
      stringRedisTemplate.expire(key, RATE_LIMIT_WINDOW);
    }
  }

  private void resetFailedAttempts(String key) {
    stringRedisTemplate.delete(key);
  }

  private String extractIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private Map<String, Object> extractDeviceInfo(HttpServletRequest request) {
    Map<String, Object> deviceInfo = new HashMap<>();
    String userAgent = request.getHeader("User-Agent");
    deviceInfo.put("userAgent", userAgent != null ? userAgent : "unknown");
    deviceInfo.put("browser", extractBrowser(userAgent));
    deviceInfo.put("os", extractOS(userAgent));
    deviceInfo.put("deviceType", detectDeviceType(userAgent));
    return deviceInfo;
  }

  private String extractBrowser(String userAgent) {
    if (userAgent == null) return "unknown";
    if (userAgent.contains("Chrome")) return "Chrome";
    if (userAgent.contains("Firefox")) return "Firefox";
    if (userAgent.contains("Safari")) return "Safari";
    if (userAgent.contains("Edge")) return "Edge";
    return "unknown";
  }

  private String extractOS(String userAgent) {
    if (userAgent == null) return "unknown";
    if (userAgent.contains("Windows")) return "Windows";
    if (userAgent.contains("Mac OS")) return "macOS";
    if (userAgent.contains("Linux")) return "Linux";
    if (userAgent.contains("Android")) return "Android";
    if (userAgent.contains("iOS")) return "iOS";
    return "unknown";
  }

  private String detectDeviceType(String userAgent) {
    if (userAgent == null) return "desktop";
    if (userAgent.contains("Mobile")) return "mobile";
    if (userAgent.contains("Tablet")) return "tablet";
    return "desktop";
  }

  private String generateMfaToken(UUID memberId) {
    String token = MFA_TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "");
    String key = "auth:mfa:token:" + token;
    stringRedisTemplate.opsForValue().set(key, memberId.toString(), Duration.ofMinutes(MFA_TOKEN_EXPIRY_MINUTES));
    return token;
  }

  private String extractMfaTokenId(String mfaToken) {
    if (mfaToken == null || !mfaToken.startsWith(MFA_TOKEN_PREFIX)) {
      return null;
    }
    String key = "auth:mfa:token:" + mfaToken;
    String memberId = stringRedisTemplate.opsForValue().get(key);
    if (memberId != null) {
      stringRedisTemplate.delete(key);
    }
    return memberId;
  }

  private AuthTokens createSessionAndTokens(OrganizationMember member, HttpServletRequest request) {
    if (request != null) {
      OrgMemberSession session = new OrgMemberSession();
      session.setMember(member);
      session.setDeviceInfo(extractDeviceInfo(request));
      session.setIpAddress(extractIpAddress(request));
      session.setExpiresAt(Instant.now().plus(SESSION_EXPIRY_HOURS, ChronoUnit.HOURS));
      orgMemberSessionRepository.save(session);
    }

    return generateTokens(member);
  }

  private AuthTokens generateTokens(OrganizationMember member) {
    long expiresIn = 1800;
    JwtClaims claims = new JwtClaims(
        member.getId().toString(),
        member.getOrganization().getId().toString(),
        null,
        member.getRole().name(),
        TYPE_ORG_MEMBER,
        null,
        "auctoritas.dev",
        Instant.now().getEpochSecond(),
        Instant.now().plusSeconds(expiresIn).getEpochSecond()
    );

    String accessToken = jwtService.generateToken(claims);
    RefreshToken refreshToken = refreshTokenService.create(member.getId(), null);

    return new AuthTokens(accessToken, refreshToken.getTokenHash(), expiresIn);
  }

  public sealed interface LoginResult {
    record Success(OrganizationMember member, AuthTokens tokens) implements LoginResult {}
    record MfaRequired(String mfaToken, String mfaTokenId) implements LoginResult {}
    record Failure(String errorCode, String message) implements LoginResult {}

    static Success success(OrganizationMember member, AuthTokens tokens) {
      return new Success(member, tokens);
    }

    static MfaRequired mfaRequired(String mfaToken, String mfaTokenId) {
      return new MfaRequired(mfaToken, mfaTokenId);
    }

    static Failure failure(String errorCode, String message) {
      return new Failure(errorCode, message);
    }
  }
}
