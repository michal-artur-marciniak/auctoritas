package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.rbac.RolePermission;
import dev.auctoritas.auth.domain.rbac.RolePermissionId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
  @Query("select rp from RolePermission rp where rp.roleId = :roleId")
  List<RolePermission> findByRoleId(@Param("roleId") UUID roleId);

  @Modifying
  @Query("delete from RolePermission rp where rp.roleId = :roleId")
  void deleteByRoleId(@Param("roleId") UUID roleId);
}
