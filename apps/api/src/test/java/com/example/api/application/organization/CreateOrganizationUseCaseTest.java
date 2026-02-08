package com.example.api.application.organization;

import com.example.api.application.organization.dto.CreateOrganizationRequest;
import com.example.api.domain.organization.Organization;
import com.example.api.domain.organization.OrganizationMember;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.organization.OrganizationRepository;
import com.example.api.domain.organization.exception.OrganizationSlugAlreadyExistsException;
import com.example.api.domain.user.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrganizationUseCaseTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CreateOrganizationUseCase useCase;

    @Test
    void throwsWhenSlugExists() {
        when(organizationRepository.findBySlug("acme")).thenReturn(Optional.of(sampleOrganization()));

        assertThrows(OrganizationSlugAlreadyExistsException.class, () ->
                useCase.execute(sampleRequest()));

        verify(memberRepository, never()).save(any(OrganizationMember.class));
    }

    @Test
    void createsOrganizationAndOwner() {
        when(organizationRepository.findBySlug("acme")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        final var response = useCase.execute(sampleRequest());

        assertEquals("Acme", response.name());
        assertEquals("acme", response.slug());
        verify(organizationRepository).save(any(Organization.class));
        verify(memberRepository).save(any(OrganizationMember.class));
    }

    private CreateOrganizationRequest sampleRequest() {
        return new CreateOrganizationRequest(
                "Acme",
                "acme",
                "owner@acme.com",
                "password123",
                "Owner"
        );
    }

    private Organization sampleOrganization() {
        return Organization.create("Acme", "acme");
    }
}
