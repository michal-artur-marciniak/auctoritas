package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserJpaEntity}.
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByStripeCustomerId(String stripeCustomerId);

    boolean existsByEmail(String email);
}
