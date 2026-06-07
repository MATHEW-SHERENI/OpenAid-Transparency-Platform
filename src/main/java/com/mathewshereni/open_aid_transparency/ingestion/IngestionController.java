package com.mathewshereni.open_aid_transparency.ingestion;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PostMapping("/recipients/world-bank")
    public IngestionResult importWorldBankRecipients() {
        return worldBankRecipientIngestionService.importSubSaharanAfricaCountries();
    }
}
