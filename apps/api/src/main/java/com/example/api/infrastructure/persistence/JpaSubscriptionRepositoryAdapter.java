package com.example.api.infrastructure.persistence;

import com.example.api.domain.subscription.Subscription;
import com.example.api.domain.subscription.SubscriptionId;
import com.example.api.domain.subscription.SubscriptionRepository;
import com.example.api.domain.user.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter implementing the domain {@link SubscriptionRepository} port.
 */
@Repository
public class JpaSubscriptionRepositoryAdapter implements SubscriptionRepository {

    private final SubscriptionJpaRepository jpaRepository;

    public JpaSubscriptionRepositoryAdapter(SubscriptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        return jpaRepository.findById(id.value())
                .map(SubscriptionDomainMapper::toDomain);
    }

    @Override
    public Optional<Subscription> findByUserId(UserId userId) {
        return jpaRepository.findByUserId(userId.value())
                .map(SubscriptionDomainMapper::toDomain);
    }

    @Override
    public Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return jpaRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .map(SubscriptionDomainMapper::toDomain);
    }

    @Override
    public Subscription save(Subscription subscription) {
        final var entity = SubscriptionDomainMapper.toEntity(subscription);
        jpaRepository.save(entity);
        return subscription;
    }

    @Override
    public void delete(SubscriptionId id) {
        jpaRepository.deleteById(id.value());
    }
}
