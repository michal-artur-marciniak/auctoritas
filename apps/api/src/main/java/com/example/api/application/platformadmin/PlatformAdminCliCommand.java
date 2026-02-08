package com.example.api.application.platformadmin;

import com.example.api.domain.platformadmin.PlatformAdmin;
import com.example.api.domain.platformadmin.PlatformAdminRepository;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * CLI command for creating the first platform admin.
 * This is a one-time setup command that can only be used when no admins exist.
 *
 * Usage: ./gradlew bootRun --args="create-admin email password name"
 */
@Component
public class PlatformAdminCliCommand implements CommandLineRunner {

    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;

    public PlatformAdminCliCommand(PlatformAdminRepository platformAdminRepository,
                                    PasswordEncoder passwordEncoder) {
        this.platformAdminRepository = platformAdminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (args.length == 0) {
            return; // No CLI command specified
        }

        if (!"create-admin".equals(args[0])) {
            return; // Not our command
        }

        if (args.length != 4) {
            System.err.println("Usage: create-admin <email> <password> <name>");
            System.exit(1);
            return;
        }

        final var email = args[1];
        final var password = args[2];
        final var name = args[3];

        createAdmin(email, password, name);
        System.exit(0);
    }

    private void createAdmin(String email, String password, String name) {
        // Check if any admins already exist
        final var adminCount = platformAdminRepository.count();
        if (adminCount > 0) {
            System.err.println("Error: Platform admins already exist. CLI creation is only allowed for the first admin.");
            System.err.println("Use the HTTP API (POST /api/platform/admin) with platform admin authentication to create additional admins.");
            System.exit(1);
            return;
        }

        // Check if email already exists (shouldn't happen if count is 0, but just in case)
        final var emailVo = new Email(email);
        if (platformAdminRepository.findByEmail(emailVo).isPresent()) {
            System.err.println("Error: Platform admin with email '" + email + "' already exists.");
            System.exit(1);
            return;
        }

        // Create the admin
        final var passwordVo = Password.create(password, passwordEncoder);
        final var admin = PlatformAdmin.create(emailVo, passwordVo, name);
        platformAdminRepository.save(admin);

        System.out.println("Platform admin created successfully:");
        System.out.println("  ID: " + admin.getId().value());
        System.out.println("  Email: " + admin.getEmail().value());
        System.out.println("  Name: " + admin.getName());
        System.out.println("  Status: " + admin.getStatus());
        System.out.println();
        System.out.println("You can now authenticate using POST /api/platform/auth/login");
    }
}
