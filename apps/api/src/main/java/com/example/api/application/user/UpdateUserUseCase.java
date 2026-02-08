package com.example.api.application.user;

import com.example.api.application.auth.dto.UserDto;
import com.example.api.application.user.dto.UpdateUserRequest;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.UserId;
import com.example.api.domain.user.UserRepository;
import com.example.api.domain.user.exception.EmailAlreadyExistsException;
import com.example.api.domain.user.exception.UserNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Use case for updating the current user's profile.
 */
@Component
public class UpdateUserUseCase {

    private final UserRepository userRepository;

    public UpdateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Updates the user's email and name, returning the updated profile.
     */
    public UserDto execute(UpdateUserRequest request) {
        final var user = userRepository.findById(UserId.of(request.userId()))
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        final var newEmail = new Email(request.email());
        if (!newEmail.value().equals(user.getEmail().value()) && userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException(request.email());
        }

        user.changeEmail(newEmail);
        user.changeName(request.name());
        userRepository.save(user);

        return UserDto.fromDomain(user);
    }
}
