package dev.auctoritas.auth.application.port.in.enduser;

import dev.auctoritas.auth.adapter.in.web.EndUserEmailVerificationRequest;
import dev.auctoritas.auth.adapter.in.web.EndUserEmailVerificationResponse;
import dev.auctoritas.auth.adapter.in.web.EndUserResendVerificationRequest;

/**
 * Use case for EndUser email verification.
 */
public interface EndUserEmailVerificationUseCase {
  EndUserEmailVerificationResponse verifyEmail(
      String apiKey, EndUserEmailVerificationRequest request);

  EndUserEmailVerificationResponse resendVerification(
      String apiKey, EndUserResendVerificationRequest request);
}
