package dev.auctoritas.auth.adapter.out.mfa;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.auctoritas.auth.application.port.out.mfa.QrCodeGeneratorPort;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Adapter for generating QR codes using ZXing library.
 */
@Component
public class QrCodeGeneratorAdapter implements QrCodeGeneratorPort {

  private static final int QR_CODE_SIZE = 200;
  private static final String IMAGE_FORMAT = "PNG";

  @Override
  public String generateQrCodeDataUrl(String secret, String accountName, String issuer) {
    try {
      String otpAuthUri = generateOtpAuthUri(secret, accountName, issuer);

      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      Map<EncodeHintType, Object> hints = new HashMap<>();
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      hints.put(EncodeHintType.MARGIN, 2);

      BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthUri, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, IMAGE_FORMAT, outputStream);

      String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());
      return "data:image/png;base64," + base64Image;
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate QR code", e);
    }
  }

  @Override
  public String generateOtpAuthUri(String secret, String accountName, String issuer) {
    String encodedAccount = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
    String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);

    return String.format(
        "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
        encodedIssuer, encodedAccount, secret, encodedIssuer
    );
  }
}
