package com.example.api.infrastructure.security;

import com.example.api.domain.user.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt adapter implementing the domain {@link PasswordEncoder} port.
 */
@Component
public class PasswordEncoderAdapter implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(String rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return bcrypt.matches(rawPassword, encodedPassword);
    }

    /**
     * Exposes the underlying Spring PasswordEncoder for SecurityConfig.
     */
    public BCryptPasswordEncoder springEncoder() {
        return bcrypt;
    }
}
