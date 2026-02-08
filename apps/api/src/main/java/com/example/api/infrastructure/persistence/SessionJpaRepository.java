package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for {@link SessionJpaEntity}.
 */
public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, String> {

    @Query("""
            select s from SessionJpaEntity s
            where s.userId = :userId
              and s.revoked = false
              and s.expiresAt > :now
            """)
    List<SessionJpaEntity> findActiveSessionsByUserId(@Param("userId") String userId,
                                                      @Param("now") Instant now);
}
