package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.enduser.EndUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

@Repository
public interface EndUserRepository extends JpaRepository<EndUser, UUID> {
  boolean existsByEmailAndProjectId(String email, UUID projectId);

  Optional<EndUser> findByEmailAndProjectId(String email, UUID projectId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select user from EndUser user where user.email = :email and user.project.id = :projectId")
  Optional<EndUser> findByEmailAndProjectIdForUpdate(
      @Param("email") String email, @Param("projectId") UUID projectId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select user from EndUser user where user.id = :userId and user.project.id = :projectId")
  Optional<EndUser> findByIdAndProjectIdForUpdate(
      @Param("userId") UUID userId, @Param("projectId") UUID projectId);
}
