package com.mathewshereni.open_aid_transparency.feedback;

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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for FeedbackController - the module with the most distinctive
 * authorization: ANY authenticated user may POST feedback (not just ADMIN), but
 * only ADMIN may DELETE it. We also confirm the author is taken from the
 * authenticated principal's name, never from the request body.
 */
@WebMvcTest(FeedbackController.class)
@Import(SecurityConfig.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService service;

    @MockitoBean
    private JwtService jwtService;

    private FeedbackResponse sample() {
        return new FeedbackResponse(100L, 4, "Helpful", Instant.parse("2024-01-01T00:00:00Z"),
                new FeedbackResponse.ProjectSummary(5L, "Clean Water"),
                new FeedbackResponse.AuthorSummary(3L, "amina"));
    }

    private static final String VALID_BODY = """
            {"projectId":5,"rating":4,"comment":"Helpful"}
            """;

    @Test
    @WithMockUser(username = "amina", roles = "CITIZEN")
    void create_allowsAnyAuthenticatedUserAndUsesPrincipalAsAuthor() throws Exception {
        when(service.create(any(FeedbackRequest.class), eq("amina"))).thenReturn(sample());

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.username").value("amina"));

        // The controller must pass the authenticated username, not anything from the body.
        verify(service).create(any(FeedbackRequest.class), eq("amina"));
    }

    @Test
    void create_returns401WhenAnonymous() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void create_returns400WhenRatingOutOfRange() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":5,"rating":9}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("rating"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns204ForAdmin() throws Exception {
        mockMvc.perform(delete("/api/feedback/100"))
                .andExpect(status().isNoContent());
        verify(service).delete(100L);
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void delete_returns403ForNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/feedback/100"))
                .andExpect(status().isForbidden());
        verify(service, never()).delete(any());
    }
}
