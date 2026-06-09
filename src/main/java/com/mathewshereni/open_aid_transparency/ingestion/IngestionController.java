package com.mathewshereni.open_aid_transparency.ingestion;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Admin-triggered data imports. Secured to ADMIN in SecurityConfig
 * (/api/ingestion/**). These are actions, so we use POST.
 */
@RestController
@RequestMapping("/api/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final WorldBankRecipientIngestionService worldBankRecipientIngestionService;
    private final WorldBankOdaIngestionService worldBankOdaIngestionService;
    private final FundingCsvImportService fundingCsvImportService;
    private final UnSdgGoalIngestionService unSdgGoalIngestionService;

    @PostMapping("/recipients/world-bank")
    public IngestionResult importWorldBankRecipients() {
        return worldBankRecipientIngestionService.importSubSaharanAfricaCountries();
    }

    /** Import/refresh the SDG goal taxonomy (titles + descriptions) from the UN SDG API. */
    @PostMapping("/sdg-goals/un")
    public IngestionResult importUnSdgGoals() {
        return unSdgGoalIngestionService.importGoals();
    }

    /**
     * Import funding flows from the World Bank ODA indicator via a Spring Batch job.
     * Year range is configurable, defaulting to a recent window. Re-runnable: the
     * job skips flows it has already imported.
     */
    @PostMapping("/funding/world-bank-oda")
    public IngestionResult importWorldBankOda(
            @RequestParam(defaultValue = "2015") int fromYear,
            @RequestParam(defaultValue = "2022") int toYear) throws Exception {
        return worldBankOdaIngestionService.importOda(fromYear, toYear);
    }

    /**
     * Bulk-import funding flows from an uploaded CSV file (multipart form field
     * "file"). Columns: donor, recipientIso, year, amount, currency.
     */
    @PostMapping(value = "/funding/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestionResult importFundingCsv(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return fundingCsvImportService.importCsv(content);
    }
}
