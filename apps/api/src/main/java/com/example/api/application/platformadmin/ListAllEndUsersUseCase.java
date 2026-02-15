package com.example.api.application.platformadmin;

import com.example.api.domain.project.ProjectId;
import com.example.api.domain.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for listing all end users across all projects.
 * Platform admins can view all end users across all tenants for support purposes.
 * Supports filtering by email search and project ID.
 */
@Component
public class ListAllEndUsersUseCase {

    private final UserRepository userRepository;

    public ListAllEndUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EndUserSummaryResponse> execute(String emailSearch, String projectId) {
        final var users = findUsers(emailSearch, projectId);

        return users.stream()
                .map(EndUserSummaryResponse::from)
                .toList();
    }

    private List<com.example.api.domain.user.User> findUsers(String emailSearch, String projectId) {
        if (emailSearch != null && !emailSearch.isBlank()) {
            return userRepository.findByEmailContainingIgnoreCase(emailSearch);
        }

        if (projectId != null && !projectId.isBlank()) {
            return userRepository.findByProjectId(ProjectId.of(projectId));
        }

        return userRepository.findAll();
    }
}
