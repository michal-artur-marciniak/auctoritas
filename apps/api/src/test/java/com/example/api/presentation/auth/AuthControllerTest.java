package com.example.api.presentation.auth;

import com.example.api.application.auth.LoginUseCase;
import com.example.api.application.auth.RefreshTokenUseCase;
import com.example.api.application.auth.RegisterUseCase;
import com.example.api.application.auth.dto.AuthResponse;
import com.example.api.application.auth.dto.UserDto;
import com.example.api.infrastructure.security.AuthCookieProperties;
import com.example.api.infrastructure.security.AuthCookieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;

    private RegisterUseCase registerUseCase;

    private LoginUseCase loginUseCase;

    private RefreshTokenUseCase refreshTokenUseCase;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        registerUseCase = Mockito.mock(RegisterUseCase.class);
        loginUseCase = Mockito.mock(LoginUseCase.class);
        refreshTokenUseCase = Mockito.mock(RefreshTokenUseCase.class);
        final var authCookieService = new AuthCookieService(new AuthCookieProperties());
        controller = new AuthController(registerUseCase, loginUseCase, refreshTokenUseCase, authCookieService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void registerReturnsCreated() throws Exception {
        when(registerUseCase.execute(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\",\"name\":\"User\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().stringValues("Set-Cookie", hasItems(
                        containsString("access_token="),
                        containsString("refresh_token=")
                )))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void loginReturnsOk() throws Exception {
        when(loginUseCase.execute(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItems(
                        containsString("access_token="),
                        containsString("refresh_token=")
                )))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void refreshReturnsOk() throws Exception {
        when(refreshTokenUseCase.execute(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItems(
                        containsString("access_token="),
                        containsString("refresh_token=")
                )))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void oauthGithubRedirects() throws Exception {
        mockMvc.perform(get("/api/auth/oauth/github"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/oauth2/authorization/github"));
    }

    @Test
    void oauthGoogleRedirects() throws Exception {
        mockMvc.perform(get("/api/auth/oauth/google"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/oauth2/authorization/google"));
    }

    private AuthResponse sampleResponse() {
        return new AuthResponse(
                "access",
                "refresh",
                new UserDto("user-id", "user@example.com", "User", "USER")
        );
    }
}
