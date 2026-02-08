package com.example.api.infrastructure.persistence;

import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.User;
import com.example.api.domain.user.UserId;

/**
 * Maps between the domain {@link User} aggregate and the JPA {@link UserJpaEntity}.
 */
final class UserDomainMapper {

    private UserDomainMapper() {
        // Utility class
    }

    /**
     * Converts a JPA entity to a domain aggregate.
     */
    static User toDomain(UserJpaEntity entity) {
        return new User(
                UserId.of(entity.getId()),
                new Email(entity.getEmail()),
                Password.fromHash(entity.getPasswordHash()),
                entity.getName(),
                entity.getRole(),
                entity.isBanned(),
                entity.getBanReason(),
                entity.getStripeCustomerId(),
                entity.getProjectId() != null ? ProjectId.of(entity.getProjectId()) : null,
                entity.getEnvironmentId() != null ? EnvironmentId.of(entity.getEnvironmentId()) : null,
                entity.getCreatedAt()
        );
    }

    /**
     * Converts a domain aggregate to a JPA entity.
     */
    static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getId().value(),
                user.getEmail().value(),
                user.getPassword().hashedValue(),
                user.getName(),
                user.getRole(),
                user.isBanned(),
                user.getBanReason(),
                user.getStripeCustomerId(),
                user.getProjectId().map(ProjectId::value).orElse(null),
                user.getEnvironmentId().map(EnvironmentId::value).orElse(null),
                user.getCreatedAt()
        );
    }
}
