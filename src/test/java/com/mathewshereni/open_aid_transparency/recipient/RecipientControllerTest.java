package com.mathewshereni.open_aid_transparency.recipient;

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
 * Web-layer tests for RecipientController. Same authorization shape as donors:
 * reads need any login, writes need ADMIN. See DonorControllerTest for the fully
 * commented template.
 */
@WebMvcTest(RecipientController.class)
@Import(SecurityConfig.class)
class RecipientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipientService service;

    @MockitoBean
    private JwtService jwtService;

    private RecipientResponse sample() {
        return new RecipientResponse(1L, "Kenya", "KEN", "Eastern Africa");
    }

    private static final String VALID_BODY = """
            {"countryName":"Kenya","isoCode":"KEN","region":"Eastern Africa"}
            """;

    @Test
    @WithMockUser(roles = "CITIZEN")
    void getAll_returns200ForAuthenticatedUser() throws Exception {
        when(service.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/recipients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].countryName").value("Kenya"))
                .andExpect(jsonPath("$[0].isoCode").value("KEN"));
    }

    @Test
    void getAll_returns401WhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/recipients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201ForAdmin() throws Exception {
        when(service.create(any(RecipientRequest.class))).thenReturn(sample());

        mockMvc.perform(post("/api/recipients")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countryName").value("Kenya"));
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void create_returns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/recipients")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns400WhenCountryNameBlank() throws Exception {
        mockMvc.perform(post("/api/recipients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"countryName":"","isoCode":"KEN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("countryName"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns409OnDuplicate() throws Exception {
        when(service.create(any(RecipientRequest.class)))
                .thenThrow(new DuplicateResourceException("A recipient 'Kenya' already exists."));

        mockMvc.perform(post("/api/recipients")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }
}
