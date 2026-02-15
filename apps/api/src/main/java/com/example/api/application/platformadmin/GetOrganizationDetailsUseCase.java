package com.example.api.application.platformadmin;

import com.example.api.application.organization.dto.OrganizationMemberResponse;
import com.example.api.application.project.ListOrganizationProjectsUseCase;
import com.example.api.application.project.dto.ProjectResponse;
import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.organization.exception.OrganizationNotFoundException;
import com.example.api.domain.organization.OrganizationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for getting detailed organization information.
 * Platform admins can view any organization's details including members and projects.
 */
@Component
public class GetOrganizationDetailsUseCase {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ListOrganizationProjectsUseCase listOrganizationProjectsUseCase;

    public GetOrganizationDetailsUseCase(OrganizationRepository organizationRepository,
                                         OrganizationMemberRepository organizationMemberRepository,
                                         ListOrganizationProjectsUseCase listOrganizationProjectsUseCase) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.listOrganizationProjectsUseCase = listOrganizationProjectsUseCase;
    }

    @Transactional(readOnly = true)
    public OrganizationDetailsResponse execute(String organizationId) {
        final var orgId = OrganizationId.of(organizationId);

        final var organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        final var members = organizationMemberRepository.findByOrganizationId(orgId)
                .stream()
                .map(OrganizationMemberResponse::from)
                .toList();

        final var projects = listOrganizationProjectsUseCase.execute(orgId);

        return OrganizationDetailsResponse.from(organization, members, projects);
    }
}
