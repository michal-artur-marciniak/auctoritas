package dev.auctoritas.auth.application.port.out.mfa;

/**
 * Port for generating QR codes for TOTP authenticator apps.
 */
public interface QrCodeGeneratorPort {

  /**
   * Generates a QR code URL for TOTP setup.
   * Returns a data URL containing a base64-encoded PNG image.
   *
   * @param secret the TOTP secret
   * @param accountName the user account name (email)
   * @param issuer the service name (project name or "Auctoritas")
   * @return data URL with base64-encoded QR code image
   */
  String generateQrCodeDataUrl(String secret, String accountName, String issuer);

  /**
   * Generates the otpauth:// URI for TOTP.
   * This URI can be used to generate a QR code or passed to authenticator apps.
   *
   * @param secret the TOTP secret
   * @param accountName the user account name (email)
   * @param issuer the service name
   * @return otpauth:// URI
   */
  String generateOtpAuthUri(String secret, String accountName, String issuer);
}
