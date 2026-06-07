package com.mathewshereni.open_aid_transparency.ingestion;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/recipients/world-bank")
    public IngestionResult importWorldBankRecipients() {
        return worldBankRecipientIngestionService.importSubSaharanAfricaCountries();
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
}
