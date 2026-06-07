package com.mathewshereni.open_aid_transparency.donor;

import com.mathewshereni.open_aid_transparency.common.exception.DuplicateResourceException;
import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer (slice) tests for DonorController - the template for the others.
 *
 * @WebMvcTest loads ONLY the web layer for this controller (no service beans, no
 * database). We import the real SecurityConfig so the actual authorization rules
 * are exercised, and replace the collaborators with mocks:
 *   - DonorService: so we control what the "business logic" returns/throws,
 *   - JwtService: required only because SecurityConfig depends on it; the JWT
 *     filter no-ops without a Bearer header, and @WithMockUser supplies identity.
 *
 * These tests answer questions unit tests can't: correct HTTP status codes, JSON
 * shape, validation 400s, and who is allowed to call what (401 vs 403 vs 200).
 */
@WebMvcTest(DonorController.class)
@Import(SecurityConfig.class)
class DonorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DonorService service;

    @MockitoBean   // satisfies SecurityConfig's constructor dependency
    private JwtService jwtService;

    private DonorResponse sampleResponse() {
        return new DonorResponse(1L, "World Bank", DonorType.MULTILATERAL, "USA");
    }

    // ---------- reads: any authenticated user ----------

    @Test
    @WithMockUser(roles = "CITIZEN")
    void getAll_returns200AndJsonForAuthenticatedUser() throws Exception {
        when(service.findAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/donors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("World Bank"))
                .andExpect(jsonPath("$[0].type").value("MULTILATERAL"));
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void getById_returns200WithBody() throws Exception {
        when(service.findById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/donors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.country").value("USA"));
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void getById_returns404WhenServiceThrowsNotFound() throws Exception {
        when(service.findById(404L))
                .thenThrow(new ResourceNotFoundException("Donor 404 not found."));

        mockMvc.perform(get("/api/donors/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Donor 404 not found."));
    }

    @Test
    void getAll_returns401WhenAnonymous() throws Exception {
        // No @WithMockUser -> no authentication -> custom entry point returns 401.
        mockMvc.perform(get("/api/donors"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- writes: ADMIN only ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201AndLocationForAdmin() throws Exception {
        when(service.create(any(DonorRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"World Bank","type":"MULTILATERAL","country":"USA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/donors/1"))
                .andExpect(jsonPath("$.name").value("World Bank"));
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void create_returns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"World Bank","type":"MULTILATERAL","country":"USA"}
                                """))
                .andExpect(status().isForbidden());

        // Authorization is enforced BEFORE the controller, so the service is untouched.
        verify(service, org.mockito.Mockito.never()).create(any());
    }

    @Test
    void create_returns401WhenAnonymous() throws Exception {
        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"World Bank","type":"MULTILATERAL","country":"USA"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns400WithFieldErrorsWhenBodyInvalid() throws Exception {
        // Blank name violates @NotBlank -> MethodArgumentNotValidException -> 400.
        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","type":"MULTILATERAL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns409WhenServiceReportsDuplicate() throws Exception {
        when(service.create(any(DonorRequest.class)))
                .thenThrow(new DuplicateResourceException("A donor named 'World Bank' already exists."));

        mockMvc.perform(post("/api/donors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"World Bank","type":"MULTILATERAL","country":"USA"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns204ForAdmin() throws Exception {
        mockMvc.perform(delete("/api/donors/1"))
                .andExpect(status().isNoContent());

        verify(service).delete(eq(1L));
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void delete_returns403ForNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/donors/1"))
                .andExpect(status().isForbidden());
    }
}
