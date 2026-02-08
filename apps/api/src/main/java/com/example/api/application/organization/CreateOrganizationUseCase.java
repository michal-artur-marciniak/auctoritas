package com.example.api.application.organization;

import com.example.api.application.organization.dto.CreateOrganizationRequest;
import com.example.api.application.organization.dto.OrganizationResponse;
import com.example.api.domain.organization.Organization;
import com.example.api.domain.organization.OrganizationMember;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.organization.OrganizationRepository;
import com.example.api.domain.organization.exception.OrganizationSlugAlreadyExistsException;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for creating an organization with an owner.
 */
@Component
public class CreateOrganizationUseCase {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateOrganizationUseCase(OrganizationRepository organizationRepository,
                                     OrganizationMemberRepository memberRepository,
                                     PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OrganizationResponse execute(CreateOrganizationRequest request) {
        if (organizationRepository.findBySlug(request.slug()).isPresent()) {
            throw new OrganizationSlugAlreadyExistsException(request.slug());
        }

        final var organization = Organization.create(request.name(), request.slug());
        final var owner = OrganizationMember.createOwner(
                organization.getId(),
                new Email(request.ownerEmail()),
                Password.create(request.ownerPassword(), passwordEncoder),
                request.ownerName()
        );

        organizationRepository.save(organization);
        memberRepository.save(owner);

        return OrganizationResponse.from(organization, owner);
    }
}
