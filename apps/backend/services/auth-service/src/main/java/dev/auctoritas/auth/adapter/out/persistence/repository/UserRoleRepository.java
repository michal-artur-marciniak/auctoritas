package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.rbac.UserRoleAssignment;
import dev.auctoritas.auth.domain.rbac.UserRoleAssignmentId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleAssignment, UserRoleAssignmentId> {
  @Query("select ur from UserRoleAssignment ur where ur.userId = :userId")
  List<UserRoleAssignment> findByUserId(@Param("userId") UUID userId);

  @Modifying
  @Query("delete from UserRoleAssignment ur where ur.userId = :userId")
  void deleteByUserId(@Param("userId") UUID userId);
}
