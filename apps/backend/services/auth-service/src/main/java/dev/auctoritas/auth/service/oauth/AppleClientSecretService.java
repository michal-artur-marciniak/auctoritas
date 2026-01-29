package dev.auctoritas.auth.service.oauth;

public interface AppleClientSecretService {
  String createClientSecret(String teamId, String keyId, String serviceId, String privateKeyPem);
}
