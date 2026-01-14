package dev.auctoritas.auth.service;

import dev.auctoritas.auth.entity.organization.OrganizationMember;
import dev.auctoritas.auth.entity.organization.RefreshToken;
import dev.auctoritas.auth.repository.OrgMemberRefreshTokenRepository;
import dev.auctoritas.auth.repository.OrganizationMemberRepository;
import dev.auctoritas.common.util.SecureRandomUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final String TOKEN_PREFIX = "rt_";
  private static final int TOKEN_EXPIRY_DAYS = 30;
  private static final int TOKEN_LENGTH_BYTES = 32;

  private final OrgMemberRefreshTokenRepository refreshTokenRepository;
  private final OrganizationMemberRepository memberRepository;

  @Transactional
  public RefreshToken create(UUID memberId, HttpServletRequest request) {
    OrganizationMember member = memberRepository.findById(memberId)
        .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

    String rawToken = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(SecureRandomUtils.generateSecureBytes(TOKEN_LENGTH_BYTES));

    String tokenHash = hashToken(rawToken);

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setMember(member);
    refreshToken.setTokenHash(tokenHash);
    refreshToken.setExpiresAt(Instant.now().plus(TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS));
    refreshToken.setRevoked(false);
    refreshToken.setUserAgent(extractUserAgent(request));
    refreshToken.setIpAddress(extractIpAddress(request));

    refreshTokenRepository.save(refreshToken);

    log.debug("Created refresh token for member: {}", memberId);
    return refreshToken;
  }

  public Optional<RefreshToken> findByToken(String token) {
    if (token == null || !token.startsWith(TOKEN_PREFIX)) {
      return Optional.empty();
    }
    String tokenHash = hashToken(token);
    return refreshTokenRepository.findByTokenHash(tokenHash);
  }

  public Optional<RefreshToken> validateAndFind(String token) {
    Optional<RefreshToken> optionalToken = findByToken(token);
    if (optionalToken.isEmpty()) {
      return Optional.empty();
    }

    RefreshToken refreshToken = optionalToken.get();
    if (refreshToken.getRevoked()) {
      log.debug("Token is revoked: {}", refreshToken.getId());
      return Optional.empty();
    }

    if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
      log.debug("Token is expired: {}", refreshToken.getId());
      return Optional.empty();
    }

    return Optional.of(refreshToken);
  }

  @Transactional
  public void revoke(String token) {
    findByToken(token).ifPresent(refreshToken -> {
      refreshToken.setRevoked(true);
      refreshTokenRepository.save(refreshToken);
      log.debug("Revoked refresh token: {}", refreshToken.getId());
    });
  }

  @Transactional
  public void revokeAllForMember(UUID memberId) {
    List<RefreshToken> activeTokens = refreshTokenRepository.findByMemberIdAndRevokedFalse(memberId);
    for (RefreshToken token : activeTokens) {
      token.setRevoked(true);
    }
    refreshTokenRepository.saveAll(activeTokens);
    log.info("Revoked {} refresh tokens for member: {}", activeTokens.size(), memberId);
  }

  @Transactional
  public RefreshToken revokeAndReplace(String token, UUID memberId) {
    Optional<RefreshToken> optionalToken = findByToken(token);
    if (optionalToken.isEmpty()) {
      throw new IllegalArgumentException("Token not found");
    }

    RefreshToken oldToken = optionalToken.get();
    oldToken.setRevoked(true);

    String newRawToken = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(SecureRandomUtils.generateSecureBytes(TOKEN_LENGTH_BYTES));
    String newTokenHash = hashToken(newRawToken);

    RefreshToken newToken = new RefreshToken();
    newToken.setMember(oldToken.getMember());
    newToken.setTokenHash(newTokenHash);
    newToken.setExpiresAt(Instant.now().plus(TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS));
    newToken.setRevoked(false);
    newToken.setUserAgent(oldToken.getUserAgent());
    newToken.setIpAddress(oldToken.getIpAddress());
    newToken.setReplacedBy(newTokenHash);

    oldToken.setReplacedBy(newTokenHash);
    refreshTokenRepository.save(oldToken);
    refreshTokenRepository.save(newToken);

    log.info("Rotated refresh token: {} -> {} for member: {}",
        oldToken.getId(), newToken.getId(), memberId);

    return newToken;
  }

  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public int cleanupExpired() {
    Instant cutoff = Instant.now();
    List<RefreshToken> expiredTokens = refreshTokenRepository.findAll().stream()
        .filter(t -> t.getExpiresAt().isBefore(cutoff))
        .toList();

    refreshTokenRepository.deleteAll(expiredTokens);
    log.info("Cleaned up {} expired refresh tokens", expiredTokens.size());
    return expiredTokens.size();
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  private String extractUserAgent(HttpServletRequest request) {
    return request.getHeader("User-Agent");
  }

  private String extractIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
