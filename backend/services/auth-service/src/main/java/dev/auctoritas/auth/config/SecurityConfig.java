package dev.auctoritas.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new Argon2PasswordEncoder(
        16, // salt length bytes
        32, // hash length bytes
        65536, // memory in KB (64MB)
        4, // parallelism
        3 // iterations
        );
  }
}
