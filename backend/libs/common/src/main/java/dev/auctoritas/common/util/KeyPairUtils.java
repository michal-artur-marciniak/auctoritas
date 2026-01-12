package dev.auctoritas.common.util;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyPairUtils {

  private static final String RSA_ALGORITHM = "RSA";

  public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
    keyGen.initialize(keySize);
    return keyGen.generateKeyPair();
  }

  public static void saveKeyPair(KeyPair keyPair, String privateKeyPath, String publicKeyPath) throws IOException {
    Files.createDirectories(Path.of(privateKeyPath).getParent());

    try (JcaPEMWriter privateWriter = new JcaPEMWriter(new FileWriter(privateKeyPath));
         JcaPEMWriter publicWriter = new JcaPEMWriter(new FileWriter(publicKeyPath))) {
      privateWriter.writeObject(keyPair.getPrivate());
      publicWriter.writeObject(keyPair.getPublic());
    }
  }

  public static PrivateKey loadPrivateKey(String path) throws Exception {
    try (PEMParser parser = new PEMParser(new FileReader(path))) {
      Object obj = parser.readObject();
      PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) obj;
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());
      KeyFactory kf = KeyFactory.getInstance(RSA_ALGORITHM);
      return kf.generatePrivate(spec);
    }
  }

  public static PublicKey loadPublicKey(String path) throws Exception {
    try (PEMParser parser = new PEMParser(new FileReader(path))) {
      Object obj = parser.readObject();
      byte[] content = ((org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) obj).getEncoded();
      X509EncodedKeySpec spec = new X509EncodedKeySpec(content);
      KeyFactory kf = KeyFactory.getInstance(RSA_ALGORITHM);
      return kf.generatePublic(spec);
    }
  }

  public static void initializeDefaultKeys(String privateKeyPath, String publicKeyPath, int keySize) {
    if (!Files.exists(Path.of(privateKeyPath)) || !Files.exists(Path.of(publicKeyPath))) {
      try {
        KeyPair keyPair = generateKeyPair(keySize);
        saveKeyPair(keyPair, privateKeyPath, publicKeyPath);
      } catch (Exception e) {
        throw new RuntimeException("Failed to generate default JWT keys", e);
      }
    }
  }
}
