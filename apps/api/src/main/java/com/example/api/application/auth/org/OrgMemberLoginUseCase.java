package com.example.api.application.auth.org;

import com.example.api.application.organization.dto.OrgAuthResponse;
import com.example.api.application.organization.dto.OrganizationMemberResponse;
import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.PasswordEncoder;
import com.example.api.domain.user.exception.InvalidCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for org member login.
 */
@Component
public class OrgMemberLoginUseCase {

    private final OrganizationMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrgTokenProvider tokenProvider;

    public OrgMemberLoginUseCase(OrganizationMemberRepository memberRepository,
                                 PasswordEncoder passwordEncoder,
                                 OrgTokenProvider tokenProvider) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public OrgAuthResponse execute(String organizationId, String email, String password) {
        final var member = memberRepository
                .findByEmailAndOrganizationId(new Email(email), OrganizationId.of(organizationId))
                .orElseThrow(InvalidCredentialsException::new);

        if (!member.getPassword().matches(password, passwordEncoder)) {
            throw new InvalidCredentialsException();
        }

        member.recordLogin();
        memberRepository.save(member);

        final var accessToken = tokenProvider.generateAccessToken(member);
        return new OrgAuthResponse(accessToken, OrganizationMemberResponse.from(member));
    }
}
