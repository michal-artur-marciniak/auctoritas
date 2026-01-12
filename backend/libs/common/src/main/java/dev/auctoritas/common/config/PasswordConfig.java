package dev.auctoritas.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.password")
public class PasswordConfig {
  private Policy policy = new Policy();

  @Data
  public static class Policy {
    private int minLength = 8;
    private int maxLength = 128;
    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigit = true;
    private boolean requireSpecialChar = true;
    private String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
    private int historyCount = 12;
    private int maxFailedAttempts = 5;
    private int lockoutDurationMinutes = 15;
  }
}
