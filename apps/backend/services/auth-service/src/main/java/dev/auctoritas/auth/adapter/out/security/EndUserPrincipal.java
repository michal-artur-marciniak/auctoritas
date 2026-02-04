package dev.auctoritas.auth.adapter.out.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Represents the authenticated end user in Spring Security context.
 */
public record EndUserPrincipal(
    UUID endUserId,
    UUID projectId,
    String email,
    List<String> roles,
    List<String> permissions)
    implements Authentication {

  public EndUserPrincipal {
    Objects.requireNonNull(endUserId, "endUserId");
    Objects.requireNonNull(projectId, "projectId");
    Objects.requireNonNull(email, "email");
    if (roles == null) {
      roles = List.of();
    }
    if (permissions == null) {
      permissions = List.of();
    }
  }

  public EndUserPrincipal(UUID endUserId, UUID projectId, String email) {
    this(endUserId, projectId, email, List.of(), List.of());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_END_USER"));
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return this;
  }

  @Override
  public boolean isAuthenticated() {
    return true;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    if (!isAuthenticated) {
      throw new IllegalArgumentException("Cannot un-authenticate end user principal");
    }
  }

  @Override
  public String getName() {
    return email;
  }
}
