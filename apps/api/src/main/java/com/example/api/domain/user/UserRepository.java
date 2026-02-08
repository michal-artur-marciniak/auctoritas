package com.example.api.domain.user;

import java.util.Optional;

/**
 * Repository port for User aggregate persistence.
 *
 * <p>Defined in the domain layer; implemented by infrastructure adapters.</p>
 */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    boolean existsByEmail(Email email);

    User save(User user);

    void delete(UserId id);
}
