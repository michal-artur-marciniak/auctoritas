package com.example.api.domain.apikey;

import com.example.api.domain.project.ProjectId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for API key persistence.
 */
public interface ApiKeyRepository {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> listByProjectId(ProjectId projectId);

    ApiKey save(ApiKey apiKey);

    void revoke(ApiKeyId id);
}
