package com.example.api.presentation.user;

import com.example.api.application.auth.dto.UserDto;
import com.example.api.application.user.GetUserUseCase;
import com.example.api.application.user.UpdateUserUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.api.presentation.user.dto.UpdateUserRequestDto;
import jakarta.validation.Valid;

/**
 * REST controller for user-related endpoints.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final GetUserUseCase getUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;

    public UserController(GetUserUseCase getUserUseCase, UpdateUserUseCase updateUserUseCase) {
        this.getUserUseCase = getUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
    }

    /**
     * Returns the currently authenticated user's profile.
     *
     * @param authentication the Spring Security authentication
     * @return user profile data
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        final var userId = (String) authentication.getPrincipal();
        final var user = getUserUseCase.execute(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Updates the currently authenticated user's profile.
     */
    @PatchMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequestDto dto) {
        final var userId = (String) authentication.getPrincipal();
        final var user = updateUserUseCase.execute(dto.toRequest(userId));
        return ResponseEntity.ok(user);
    }

    /**
     * Returns an admin-only marker response.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/check")
    public ResponseEntity<String> adminCheck() {
        return ResponseEntity.ok("admin-ok");
    }
}
