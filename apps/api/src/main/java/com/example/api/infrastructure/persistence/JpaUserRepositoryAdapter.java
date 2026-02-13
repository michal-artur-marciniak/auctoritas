package com.example.api.infrastructure.persistence;

import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.User;
import com.example.api.domain.user.UserId;
import com.example.api.domain.user.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter implementing the domain {@link UserRepository} port.
 */
@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public JpaUserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.value())
                .map(UserDomainMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value())
                .map(UserDomainMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmailAndProjectId(Email email, ProjectId projectId, EnvironmentId environmentId) {
        return jpaRepository.findByEmailAndProjectIdAndEnvironmentId(
                        email.value(), projectId.value(), environmentId.value())
                .map(UserDomainMapper::toDomain);
    }

    @Override
    public Optional<User> findByIdAndProjectId(UserId id, ProjectId projectId, EnvironmentId environmentId) {
        return jpaRepository.findByIdAndProjectIdAndEnvironmentId(
                        id.value(), projectId.value(), environmentId.value())
                .map(UserDomainMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }

    @Override
    public User save(User user) {
        final var entity = UserDomainMapper.toEntity(user);
        jpaRepository.save(entity);
        return user;
    }

    @Override
    public void delete(UserId id) {
        jpaRepository.deleteById(id.value());
    }
}
