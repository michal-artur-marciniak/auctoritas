package dev.auctoritas.auth.application.port.in.org;

import dev.auctoritas.auth.adapter.in.web.OrgLoginRequest;
import dev.auctoritas.auth.adapter.in.web.OrgLoginResponse;
import dev.auctoritas.auth.adapter.in.web.OrgRefreshRequest;
import dev.auctoritas.auth.adapter.in.web.OrgRefreshResponse;

/**
 * Use case for Organization authentication (login and refresh).
 */
public interface OrgAuthUseCase {
  OrgLoginResponse login(OrgLoginRequest request);

  OrgRefreshResponse refresh(OrgRefreshRequest request);
}
