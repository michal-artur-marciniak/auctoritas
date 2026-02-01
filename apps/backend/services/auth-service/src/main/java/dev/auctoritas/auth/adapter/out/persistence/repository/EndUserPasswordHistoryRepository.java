package dev.auctoritas.auth.adapter.out.persistence.repository;

import dev.auctoritas.auth.domain.enduser.EndUserPasswordHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EndUserPasswordHistoryRepository
    extends JpaRepository<EndUserPasswordHistory, UUID> {
  @Query(
      "select h from EndUserPasswordHistory h "
          + "where h.project.id = :projectId and h.user.id = :userId "
          + "order by h.createdAt desc")
  List<EndUserPasswordHistory> findRecent(
      @Param("projectId") UUID projectId, @Param("userId") UUID userId, Pageable pageable);
}
