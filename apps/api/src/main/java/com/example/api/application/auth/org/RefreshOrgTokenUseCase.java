package com.example.api.application.auth.org;

import com.example.api.application.organization.dto.OrgAuthResponse;
import com.example.api.application.organization.dto.OrganizationMemberResponse;
import com.example.api.domain.organization.OrganizationMemberId;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.user.exception.InvalidTokenException;
import org.springframework.stereotype.Component;

/**
 * Use case for refreshing org member access token.
 */
@Component
public class RefreshOrgTokenUseCase {

    private final OrganizationMemberRepository memberRepository;
    private final OrgTokenProvider tokenProvider;

    public RefreshOrgTokenUseCase(OrganizationMemberRepository memberRepository,
                                  OrgTokenProvider tokenProvider) {
        this.memberRepository = memberRepository;
        this.tokenProvider = tokenProvider;
    }

    public OrgAuthResponse execute(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        final var memberId = tokenProvider.getMemberIdFromToken(refreshToken);
        final var member = memberRepository
                .findById(OrganizationMemberId.of(memberId))
                .orElseThrow(InvalidTokenException::new);

        final var newAccessToken = tokenProvider.generateAccessToken(member);
        final var newRefreshToken = tokenProvider.generateRefreshToken(member);

        return new OrgAuthResponse(newAccessToken, newRefreshToken, OrganizationMemberResponse.from(member));
    }
}
