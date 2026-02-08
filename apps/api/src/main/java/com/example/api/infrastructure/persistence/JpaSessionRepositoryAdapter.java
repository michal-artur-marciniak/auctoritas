package com.example.api.infrastructure.persistence;

import com.example.api.domain.session.Session;
import com.example.api.domain.session.SessionId;
import com.example.api.domain.session.SessionRepository;
import com.example.api.domain.user.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing the domain {@link SessionRepository} port.
 */
@Repository
public class JpaSessionRepositoryAdapter implements SessionRepository {

    private final SessionJpaRepository jpaRepository;

    public JpaSessionRepositoryAdapter(SessionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Session> findById(SessionId id) {
        return jpaRepository.findById(id.value())
                .map(SessionDomainMapper::toDomain);
    }

    @Override
    public List<Session> findActiveSessionsByUserId(UserId userId) {
        return jpaRepository.findActiveSessionsByUserId(userId.value(), Instant.now())
                .stream()
                .map(SessionDomainMapper::toDomain)
                .toList();
    }

    @Override
    public Session save(Session session) {
        final var entity = SessionDomainMapper.toEntity(session);
        jpaRepository.save(entity);
        return session;
    }

    @Override
    public void delete(SessionId id) {
        jpaRepository.deleteById(id.value());
    }
}
