package com.example.api.domain.user;

import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root for the User domain.
 *
 * <p>Contains business rules and protects its own invariants.
 * No framework dependencies.</p>
 */
public class User {

    private final UserId id;
    private Email email;
    private Password password;
    private String name;
    private Role role;
    private boolean banned;
    private String banReason;
    private final ProjectId projectId;
    private final EnvironmentId environmentId;
    private final LocalDateTime createdAt;

    /**
     * Full constructor for reconstitution from persistence.
     */
    public User(UserId id, Email email, Password password, String name,
                Role role, boolean banned, String banReason,
                ProjectId projectId, EnvironmentId environmentId, LocalDateTime createdAt) {
        Objects.requireNonNull(id, "User ID required");
        Objects.requireNonNull(email, "Email required");
        Objects.requireNonNull(password, "Password required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name required");
        }
        Objects.requireNonNull(role, "Role required");
        Objects.requireNonNull(createdAt, "Created timestamp required");

        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.banned = banned;
        this.banReason = banReason;
        this.projectId = projectId;
        this.environmentId = environmentId;
        this.createdAt = createdAt;
    }

    /**
     * Factory method for registering a new user.
     */
    public static User register(Email email, Password password, String name) {
        return new User(
                UserId.generate(),
                email,
                password,
                name,
                Role.USER,
                false,
                null,
                null,
                null,
                LocalDateTime.now()
        );
    }

    /**
     * Factory method for registering a new SDK end user scoped to a project/environment.
     */
    public static User registerEndUser(Email email, Password password, String name,
                                       ProjectId projectId, EnvironmentId environmentId) {
        Objects.requireNonNull(projectId, "Project ID required");
        Objects.requireNonNull(environmentId, "Environment ID required");
        return new User(
                UserId.generate(),
                email,
                password,
                name,
                Role.USER,
                false,
                null,
                projectId,
                environmentId,
                LocalDateTime.now()
        );
    }

    // --- Business operations ---

    /**
     * Changes the user's password.
     */
    public void changePassword(Password newPassword) {
        Objects.requireNonNull(newPassword, "Password required");
        this.password = newPassword;
    }

    /**
     * Bans the user with a reason.
     */
    public void ban(String reason) {
        this.banned = true;
        this.banReason = reason;
    }

    /**
     * Unbans the user.
     */
    public void unban() {
        this.banned = false;
        this.banReason = null;
    }

    /**
     * Assigns a new role to the user.
     */
    public void assignRole(Role role) {
        Objects.requireNonNull(role, "Role required");
        this.role = role;
    }

    /**
     * Checks if the user is allowed to login.
     */
    public boolean canLogin() {
        return !banned;
    }

    // --- Accessors ---

    public UserId getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    /**
     * Changes the user's email address.
     */
    public void changeEmail(Email newEmail) {
        Objects.requireNonNull(newEmail, "Email required");
        this.email = newEmail;
    }

    public Password getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    /**
     * Changes the user's display name.
     */
    public void changeName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name required");
        }
        this.name = newName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isBanned() {
        return banned;
    }

    public String getBanReason() {
        return banReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Optional<ProjectId> getProjectId() {
        return Optional.ofNullable(projectId);
    }

    public Optional<EnvironmentId> getEnvironmentId() {
        return Optional.ofNullable(environmentId);
    }

    /**
     * Checks if this user belongs to the given project and environment.
     */
    public boolean belongsTo(ProjectId projectId, EnvironmentId environmentId) {
        return this.projectId != null
                && this.environmentId != null
                && this.projectId.equals(projectId)
                && this.environmentId.equals(environmentId);
    }
}
