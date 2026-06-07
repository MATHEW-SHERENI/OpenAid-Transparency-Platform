package com.mathewshereni.open_aid_transparency.funding;

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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for FundingFlowController. ADMIN-only writes, PLUS the one
 * genuinely public endpoint in the app: GET /api/funding-flows/reports/**, which
 * must be reachable with no authentication at all.
 */
@WebMvcTest(FundingFlowController.class)
@Import(SecurityConfig.class)
class FundingFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FundingFlowService service;

    @MockitoBean
    private ReportExportService reportExportService;

    @MockitoBean
    private JwtService jwtService;

    private FundingFlowResponse sample() {
        return new FundingFlowResponse(1L, new BigDecimal("5000000.00"), "USD", 2024, null,
                new FundingFlowResponse.DonorSummary(1L, "World Bank"),
                new FundingFlowResponse.RecipientSummary(2L, "Kenya"),
                null);
    }

    private static final String VALID_BODY = """
            {"amount":5000000.00,"currency":"USD","year":2024,"donorId":1,"recipientId":2}
            """;

    @Test
    void reports_arePublicAndReachableWithoutAuth() throws Exception {
        // SecurityConfig permits GET /api/funding-flows/reports/** for everyone.
        when(service.totalByRecipient()).thenReturn(List.of());

        mockMvc.perform(get("/api/funding-flows/reports/by-recipient"))
                .andExpect(status().isOk());
    }

    @Test
    void csvExport_isPublicWithAttachmentHeaderAndCsvContentType() throws Exception {
        when(service.totalByRecipient()).thenReturn(List.of());
        when(reportExportService.toCsv(List.of())).thenReturn("header\r\n".getBytes());

        mockMvc.perform(get("/api/funding-flows/reports/by-recipient.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"funding-by-recipient.csv\""));
    }

    @Test
    void pdfExport_isPublicWithPdfContentType() throws Exception {
        when(service.totalByRecipient()).thenReturn(List.of());
        when(reportExportService.toPdf(List.of())).thenReturn("%PDF-1.4".getBytes());

        mockMvc.perform(get("/api/funding-flows/reports/by-recipient.pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"funding-by-recipient.pdf\""));
    }

    @Test
    void list_requiresAuthentication() throws Exception {
        // The plain list, by contrast, is NOT public.
        mockMvc.perform(get("/api/funding-flows"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void list_returns200ForAuthenticatedUser() throws Exception {
        when(service.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/funding-flows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].donor.name").value("World Bank"))
                .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201ForAdmin() throws Exception {
        when(service.create(any(FundingFlowRequest.class))).thenReturn(sample());

        mockMvc.perform(post("/api/funding-flows")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipient.countryName").value("Kenya"));
    }

    @Test
    @WithMockUser(roles = "CITIZEN")
    void create_returns403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/funding-flows")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns400WhenCurrencyInvalid() throws Exception {
        // currency must be exactly 3 letters.
        mockMvc.perform(post("/api/funding-flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1000,"currency":"DOLLARS","year":2024,"donorId":1,"recipientId":2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currency"));
    }
}
