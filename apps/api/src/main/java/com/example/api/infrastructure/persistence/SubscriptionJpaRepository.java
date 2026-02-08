package com.example.api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link SubscriptionJpaEntity}.
 */
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, String> {

    Optional<SubscriptionJpaEntity> findByUserId(String userId);

    Optional<SubscriptionJpaEntity> findByStripeSubscriptionId(String stripeSubscriptionId);
}
