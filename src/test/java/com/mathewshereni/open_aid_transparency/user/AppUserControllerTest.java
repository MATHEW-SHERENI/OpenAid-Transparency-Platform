package com.mathewshereni.open_aid_transparency.user;

import com.mathewshereni.open_aid_transparency.common.exception.DuplicateResourceException;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for AppUserController. User management is ADMIN-only for writes.
 * We confirm a created user's response never leaks credentials, and the standard
 * 401/403/400/409 behaviours.
 *
 * NOTE: GET /api/users currently falls under "anyRequest().authenticated()", so any
 * logged-in user can list users. We assert only the unambiguous cases here (ADMIN
 * read OK, anonymous blocked) pending a decision on whether reads should be ADMIN-only.
 */
@WebMvcTest(AppUserController.class)
@Import(SecurityConfig.class)
class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService service;

    @MockitoBean
    private JwtService jwtService;

    private AppUserResponse sample() {
        return new AppUserResponse(1L, "amina", "amina@x.io", UserRole.CITIZEN, true,
                Instant.parse("2024-01-01T00:00:00Z"));
    }

    private static final String VALID_BODY = """
            {"username":"amina","email":"amina@x.io","password":"supersecret","role":"CITIZEN"}
            """;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_returns200ForAdmin() throws Exception {
        when(service.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("amina"))
                // The response shape has no password / passwordHash field at all.
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getAll_returns401WhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201ForAdmin() throws Exception {
        when(service.create(any(AppUserRequest.class))).thenReturn(sample());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("amina"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void create_returns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any());
    }

    @Test
    void create_returns401WhenAnonymous() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"amina","email":"amina@x.io","password":"x","role":"CITIZEN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns409OnDuplicateUsername() throws Exception {
        when(service.create(any(AppUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username 'amina' is already taken."));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }
}
