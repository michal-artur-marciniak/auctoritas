package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.entity.enduser.EndUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EndUserRepository extends JpaRepository<EndUser, UUID> {
  boolean existsByEmailAndProjectId(String email, UUID projectId);

  Optional<EndUser> findByEmailAndProjectId(String email, UUID projectId);
}
