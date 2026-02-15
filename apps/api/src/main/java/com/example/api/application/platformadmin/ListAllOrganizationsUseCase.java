package com.example.api.application.platformadmin;

import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.organization.OrganizationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for listing all organizations with member counts.
 * Platform admins can view all organizations across all tenants.
 */
@Component
public class ListAllOrganizationsUseCase {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public ListAllOrganizationsUseCase(OrganizationRepository organizationRepository,
                                       OrganizationMemberRepository organizationMemberRepository) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationSummaryResponse> execute() {
        final var organizations = organizationRepository.findAll();

        return organizations.stream()
                .map(org -> {
                    final var memberCount = organizationMemberRepository
                            .findByOrganizationId(org.getId())
                            .size();
                    return OrganizationSummaryResponse.from(org, memberCount);
                })
                .toList();
    }
}
