package com.example.api.presentation.user;

import com.example.api.application.auth.dto.UserDto;
import com.example.api.application.user.GetUserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.example.api.application.user.UpdateUserUseCase;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private MockMvc mockMvc;

    private GetUserUseCase getUserUseCase;

    private UserController controller;

    @BeforeEach
    void setUp() {
        getUserUseCase = Mockito.mock(GetUserUseCase.class);
        final var updateUserUseCase = Mockito.mock(UpdateUserUseCase.class);
        controller = new UserController(getUserUseCase, updateUserUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getCurrentUserRequiresAuth() throws Exception {
        when(getUserUseCase.execute("user-id"))
                .thenReturn(new UserDto("user-id", "user@example.com", "User", "USER"));

        final var auth = new UsernamePasswordAuthenticationToken(
                "user-id",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(get("/api/user/me")
                        .principal(auth)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-id"));
    }

    @Test
    void adminEndpointRespondsOkWithoutSecurityFilter() throws Exception {
        mockMvc.perform(get("/api/user/admin/check"))
                .andExpect(status().isOk());
    }
}
