package dev.auctoritas.auth.application.oauth;

public interface AppleClientSecretService {
  String createClientSecret(String teamId, String keyId, String serviceId, String privateKeyPem);
}
