package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.organization.OrganizationMemberMfa;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for OrganizationMemberMfa entities.
 */
@Repository
public interface OrganizationMemberMfaRepository extends JpaRepository<OrganizationMemberMfa, UUID> {

  Optional<OrganizationMemberMfa> findByMemberId(UUID memberId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT m FROM OrganizationMemberMfa m WHERE m.member.id = :memberId")
  Optional<OrganizationMemberMfa> findByMemberIdForUpdate(@Param("memberId") UUID memberId);

  boolean existsByMemberIdAndEnabledTrue(UUID memberId);

  void deleteByMemberId(UUID memberId);
}
