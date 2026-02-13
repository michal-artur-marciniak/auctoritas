package com.example.api.infrastructure.persistence;

import com.example.api.domain.subscription.Subscription;
import com.example.api.domain.subscription.SubscriptionId;
import com.example.api.domain.user.UserId;

/**
 * Maps between the domain {@link Subscription} and the JPA {@link SubscriptionJpaEntity}.
 */
final class SubscriptionDomainMapper {

    private SubscriptionDomainMapper() {
        // Utility class
    }

    static Subscription toDomain(SubscriptionJpaEntity entity) {
        return new Subscription(
                SubscriptionId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                entity.getPlan(),
                entity.getStatus(),
                entity.getCurrentPeriodEnd()
        );
    }

    static SubscriptionJpaEntity toEntity(Subscription subscription) {
        return new SubscriptionJpaEntity(
                subscription.getId().value(),
                subscription.getUserId().value(),
                subscription.getPlan(),
                subscription.getStatus(),
                subscription.getCurrentPeriodEnd()
        );
    }
}
