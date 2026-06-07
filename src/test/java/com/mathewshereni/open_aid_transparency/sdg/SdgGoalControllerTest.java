package com.mathewshereni.open_aid_transparency.sdg;

import com.mathewshereni.open_aid_transparency.config.SecurityConfig;
import com.mathewshereni.open_aid_transparency.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for SdgGoalController. This is read-only reference data; there
 * are no writes, so the points to confirm are: reads require a login, and a
 * lookup for an unknown goal number returns 404 (raised as ResponseStatusException
 * by the service, handled by Spring's default resolver).
 */
@WebMvcTest(SdgGoalController.class)
@Import(SecurityConfig.class)
class SdgGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SdgGoalService service;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "CITIZEN")
    void getAll_returns200ForAuthenticatedUser() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new SdgGoalDto(6L, 6, "Clean Water and Sanitation", "desc")));

        mockMvc.perform(get("/api/sdg-goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].goalNumber").value(6))
                .andExpect(jsonPath("$[0].title").value("Clean Water and Sanitation"));
    }

    @Test
    void getAll_returns401WhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/sdg-goals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void getByNumber_returns404ForUnknownGoal() throws Exception {
        when(service.findByGoalNumber(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "SDG goal 99 not found"));

        mockMvc.perform(get("/api/sdg-goals/99"))
                .andExpect(status().isNotFound());
    }
}
