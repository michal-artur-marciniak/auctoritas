package com.example.api.application.auth.org;

import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationMember;
import com.example.api.domain.organization.OrganizationMemberId;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.organization.OrganizationMemberRole;
import com.example.api.domain.organization.OrganizationStatus;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import com.example.api.domain.user.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgMemberLoginUseCaseTest {

    @Mock
    private OrganizationMemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OrgTokenProvider tokenProvider;

    @InjectMocks
    private OrgMemberLoginUseCase useCase;

    @Test
    void throwsWhenMemberMissing() {
        when(memberRepository.findByEmailAndOrganizationId(any(), any())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () ->
                useCase.execute("org-id", "owner@acme.com", "password"));

        verify(tokenProvider, never()).generateAccessToken(any());
    }

    @Test
    void throwsWhenPasswordInvalid() {
        final var member = sampleMember();
        when(memberRepository.findByEmailAndOrganizationId(any(), any())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("bad", member.getPassword().hashedValue())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
                useCase.execute("org-id", "owner@acme.com", "bad"));

        verify(tokenProvider, never()).generateAccessToken(any());
    }

    @Test
    void returnsTokenOnSuccess() {
        final var member = sampleMember();
        when(memberRepository.findByEmailAndOrganizationId(any(), any())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password123", member.getPassword().hashedValue())).thenReturn(true);
        when(tokenProvider.generateAccessToken(member)).thenReturn("org-token");

        final var response = useCase.execute("org-id", "owner@acme.com", "password123");

        assertEquals("org-token", response.accessToken());
        assertEquals("owner@acme.com", response.member().email());
        verify(memberRepository).save(member);
    }

    private OrganizationMember sampleMember() {
        return new OrganizationMember(
                OrganizationMemberId.of("member-id"),
                OrganizationId.of("org-id"),
                new Email("owner@acme.com"),
                Password.fromHash("hashed"),
                "Owner",
                OrganizationMemberRole.OWNER,
                false,
                OrganizationStatus.ACTIVE,
                null,
                LocalDateTime.now(),
                null
        );
    }
}
