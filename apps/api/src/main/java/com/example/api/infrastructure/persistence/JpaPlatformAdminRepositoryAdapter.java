package com.example.api.infrastructure.persistence;

import com.example.api.domain.platformadmin.PlatformAdmin;
import com.example.api.domain.platformadmin.PlatformAdminId;
import com.example.api.domain.platformadmin.PlatformAdminRepository;
import com.example.api.domain.platformadmin.PlatformAdminStatus;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA adapter implementing the domain {@link PlatformAdminRepository} port.
 */
@Component
public class JpaPlatformAdminRepositoryAdapter implements PlatformAdminRepository {

    private final PlatformAdminJpaRepository jpaRepository;

    public JpaPlatformAdminRepositoryAdapter(PlatformAdminJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PlatformAdmin> findById(PlatformAdminId id) {
        return jpaRepository.findById(id.value())
                .map(this::toDomain);
    }

    @Override
    public Optional<PlatformAdmin> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value())
                .map(this::toDomain);
    }

    @Override
    public long countByStatus(PlatformAdminStatus status) {
        return jpaRepository.countByStatus(status.name());
    }

    @Override
    public PlatformAdmin save(PlatformAdmin admin) {
        final var entity = toEntity(admin);
        jpaRepository.save(entity);
        return admin;
    }

    @Override
    public void delete(PlatformAdminId id) {
        jpaRepository.deleteById(id.value());
    }

    private PlatformAdmin toDomain(PlatformAdminJpaEntity entity) {
        return new PlatformAdmin(
                new PlatformAdminId(entity.getId()),
                new Email(entity.getEmail()),
                Password.fromHash(entity.getPasswordHash()),
                entity.getName(),
                entity.getStatus(),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PlatformAdminJpaEntity toEntity(PlatformAdmin admin) {
        return new PlatformAdminJpaEntity(
                admin.getId().value(),
                admin.getEmail().value(),
                admin.getPassword().hashedValue(),
                admin.getName(),
                admin.getStatus(),
                admin.getLastLoginAt(),
                admin.getCreatedAt(),
                admin.getUpdatedAt()
        );
    }
}
