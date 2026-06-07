package com.mathewshereni.open_aid_transparency.project;

import com.mathewshereni.open_aid_transparency.config.SecurityConfig;
import com.mathewshereni.open_aid_transparency.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for AidProjectController. ADMIN-only writes, plus the project-
 * specific case: a date-rule violation surfaces from the service as an
 * IllegalArgumentException, which the GlobalExceptionHandler renders as 400.
 */
@WebMvcTest(AidProjectController.class)
@Import(SecurityConfig.class)
class AidProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AidProjectService service;

    @MockitoBean
    private JwtService jwtService;

    private AidProjectResponse sample() {
        return new AidProjectResponse(1L, "Clean Water", "desc", ProjectStatus.ACTIVE,
                null, null,
                new AidProjectResponse.RecipientSummary(2L, "Kenya"),
                List.of());
    }

    private static final String VALID_BODY = """
            {"title":"Clean Water","status":"ACTIVE","recipientId":2,"sdgGoalNumbers":[6]}
            """;

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201ForAdmin() throws Exception {
        when(service.create(any(AidProjectRequest.class))).thenReturn(sample());

        mockMvc.perform(post("/api/aid-projects")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Clean Water"))
                .andExpect(jsonPath("$.recipient.countryName").value("Kenya"));
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void create_returns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/aid-projects")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns400WhenServiceRejectsDateRule() throws Exception {
        // The service raises this when endDate precedes startDate.
        when(service.create(any(AidProjectRequest.class)))
                .thenThrow(new IllegalArgumentException("endDate must not be before startDate."));

        mockMvc.perform(post("/api/aid-projects")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("endDate must not be before startDate."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns400WhenTitleBlank() throws Exception {
        mockMvc.perform(post("/api/aid-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","status":"ACTIVE","recipientId":2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }
}
