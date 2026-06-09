package com.mathewshereni.open_aid_transparency.ingestion;

import com.mathewshereni.open_aid_transparency.config.SecurityConfig;
import com.mathewshereni.open_aid_transparency.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the CSV upload endpoint. Imports are ADMIN-only, and the
 * upload is multipart/form-data with a "file" part.
 */
@WebMvcTest(IngestionController.class)
@Import(SecurityConfig.class)
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private WorldBankRecipientIngestionService recipientService;
    @MockitoBean private WorldBankOdaIngestionService odaService;
    @MockitoBean private FundingCsvImportService csvService;
    @MockitoBean private UnSdgGoalIngestionService unSdgService;
    @MockitoBean private JwtService jwtService;

    private MockMultipartFile csvFile() {
        return new MockMultipartFile("file", "flows.csv", "text/csv",
                "donor,recipientIso,year,amount,currency\nGates Foundation,KEN,2023,100,USD\n".getBytes());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void csvUpload_returns200AndSummaryForAdmin() throws Exception {
        when(csvService.importCsv(anyString())).thenReturn(new IngestionResult(1, 1, 0));

        mockMvc.perform(multipart("/api/ingestion/funding/csv").file(csvFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fetched").value(1))
                .andExpect(jsonPath("$.created").value(1));
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void csvUpload_returns403ForNonAdmin() throws Exception {
        mockMvc.perform(multipart("/api/ingestion/funding/csv").file(csvFile()))
                .andExpect(status().isForbidden());
        verify(csvService, never()).importCsv(anyString());
    }

    @Test
    void csvUpload_returns401WhenAnonymous() throws Exception {
        mockMvc.perform(multipart("/api/ingestion/funding/csv").file(csvFile()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unSdgImport_returns200AndSummaryForAdmin() throws Exception {
        when(unSdgService.importGoals()).thenReturn(new IngestionResult(17, 0, 17));

        mockMvc.perform(post("/api/ingestion/sdg-goals/un"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fetched").value(17));
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void unSdgImport_returns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/ingestion/sdg-goals/un"))
                .andExpect(status().isForbidden());
        verify(unSdgService, never()).importGoals();
    }
}
