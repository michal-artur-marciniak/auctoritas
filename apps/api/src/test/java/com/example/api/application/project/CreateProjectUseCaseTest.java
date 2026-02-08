package com.example.api.application.project;

import com.example.api.application.project.dto.CreateProjectRequest;
import com.example.api.domain.apikey.ApiKeyRepository;
import com.example.api.domain.environment.Environment;
import com.example.api.domain.environment.EnvironmentRepository;
import com.example.api.domain.environment.EnvironmentType;
import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.project.Project;
import com.example.api.domain.project.ProjectRepository;
import com.example.api.domain.project.exception.ProjectSlugAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProjectUseCaseTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private CreateProjectUseCase useCase;

    @Test
    void throwsWhenSlugExists() {
        final var orgId = OrganizationId.generate();
        final var existingProject = Project.create(orgId, "Existing", "my-project", "Desc");
        
        when(projectRepository.findBySlugAndOrganizationId("my-project", orgId))
                .thenReturn(Optional.of(existingProject));

        final var request = new CreateProjectRequest(orgId, "My Project", "my-project", "Description");
        
        assertThrows(ProjectSlugAlreadyExistsException.class, () ->
                useCase.execute(request));

        verify(environmentRepository, never()).save(any(Environment.class));
    }

    @Test
    void createsProjectWithEnvironmentsAndApiKeys() {
        final var orgId = OrganizationId.generate();
        
        when(projectRepository.findBySlugAndOrganizationId("my-project", orgId))
                .thenReturn(Optional.empty());

        final var request = new CreateProjectRequest(orgId, "My Project", "my-project", "Description");
        final var response = useCase.execute(request);

        assertNotNull(response);
        assertEquals("My Project", response.name());
        assertEquals("my-project", response.slug());
        assertEquals("Description", response.description());
        assertEquals("ACTIVE", response.status());

        verify(projectRepository).save(any(Project.class));
        verify(environmentRepository, times(2)).save(any(Environment.class));
        verify(apiKeyRepository, times(2)).save(any());

        assertEquals(2, response.environments().size());
        assertTrue(response.environments().stream()
                .anyMatch(e -> e.environmentType().equals("PROD")));
        assertTrue(response.environments().stream()
                .anyMatch(e -> e.environmentType().equals("DEV")));

        assertEquals(2, response.apiKeys().size());
        assertTrue(response.apiKeys().stream()
                .anyMatch(k -> k.environmentType().equals("PROD") && k.rawKey() != null));
        assertTrue(response.apiKeys().stream()
                .anyMatch(k -> k.environmentType().equals("DEV") && k.rawKey() != null));
    }
}
