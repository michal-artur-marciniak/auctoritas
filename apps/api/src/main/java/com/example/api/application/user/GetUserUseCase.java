package com.example.api.application.user;

import com.example.api.application.auth.dto.UserDto;
import com.example.api.domain.user.UserId;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.UserNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Use case for retrieving the current authenticated user.
 */
@Component
public class GetUserUseCase {

    private final UserRepository userRepository;

    public GetUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the user data for the given user ID.
     *
     * @param userId the user's ID
     * @return user DTO
     * @throws UserNotFoundException if no user exists with the given ID
     */
    public UserDto execute(String userId) {
        final var user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new UserNotFoundException(userId));

        return UserDto.fromDomain(user);
    }
}
