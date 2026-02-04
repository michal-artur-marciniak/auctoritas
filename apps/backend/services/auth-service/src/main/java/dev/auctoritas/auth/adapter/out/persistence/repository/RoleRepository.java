package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.rbac.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByNameAndProjectId(String name, UUID projectId);

  @Query("select r from Role r where r.project.id = :projectId")
  List<Role> findAllByProjectId(@Param("projectId") UUID projectId);
}
