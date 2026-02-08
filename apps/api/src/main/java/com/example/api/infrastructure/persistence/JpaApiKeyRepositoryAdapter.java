package com.example.api.infrastructure.persistence;

import com.example.api.domain.apikey.ApiKey;
import com.example.api.domain.apikey.ApiKeyId;
import com.example.api.domain.apikey.ApiKeyRepository;
import com.example.api.domain.project.ProjectId;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing the domain {@link ApiKeyRepository} port.
 */
@Repository
public class JpaApiKeyRepositoryAdapter implements ApiKeyRepository {

    private final ApiKeyJpaRepository jpaRepository;

    public JpaApiKeyRepositoryAdapter(ApiKeyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ApiKey> findByKeyHash(String keyHash) {
        return jpaRepository.findByKeyHash(keyHash)
                .map(ApiKeyDomainMapper::toDomain);
    }

    @Override
    public List<ApiKey> listByProjectId(ProjectId projectId) {
        return jpaRepository.findByProjectId(projectId.value())
                .stream()
                .map(ApiKeyDomainMapper::toDomain)
                .toList();
    }

    @Override
    public ApiKey save(ApiKey apiKey) {
        final var entity = ApiKeyDomainMapper.toEntity(apiKey);
        jpaRepository.save(entity);
        return apiKey;
    }

    @Override
    public void revoke(ApiKeyId id) {
        jpaRepository.findById(id.value()).ifPresent(entity -> {
            entity.setRevokedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }
}
