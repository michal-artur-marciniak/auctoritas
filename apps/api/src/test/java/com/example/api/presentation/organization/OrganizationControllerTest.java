package com.example.api.presentation.organization;

import com.example.api.application.organization.CreateOrganizationUseCase;
import com.example.api.application.organization.dto.OrganizationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationControllerTest {

    private MockMvc mockMvc;
    private CreateOrganizationUseCase createOrganizationUseCase;

    @BeforeEach
    void setUp() {
        createOrganizationUseCase = Mockito.mock(CreateOrganizationUseCase.class);
        final var controller = new OrganizationController(createOrganizationUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void registerReturnsCreated() throws Exception {
        when(createOrganizationUseCase.execute(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/org/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\",\"slug\":\"acme\",\"ownerEmail\":\"owner@acme.com\",\"ownerPassword\":\"password123\",\"ownerName\":\"Owner\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug", is("acme")))
                .andExpect(jsonPath("$.ownerEmail", is("owner@acme.com")));
    }

    private OrganizationResponse sampleResponse() {
        return new OrganizationResponse(
                "org-id",
                "Acme",
                "acme",
                "ACTIVE",
                "owner-id",
                "owner@acme.com",
                "Owner",
                java.time.LocalDateTime.now()
        );
    }
}
