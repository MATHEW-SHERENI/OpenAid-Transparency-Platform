package com.mathewshereni.open_aid_transparency.ingestion;

import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.recipient.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Imports Sub-Saharan African countries from the World Bank API into our
 * recipients table. This is "idempotent": running it again skips countries we
 * already have, so it never creates duplicates.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorldBankRecipientIngestionService {

    private final RestClient worldBankRestClient;
    private final RecipientRepository recipientRepository;

    @Transactional
    public IngestionResult importSubSaharanAfricaCountries() {
        // 1. Call the API. The body is [ {metadata}, [ {country}, ... ] ].
        JsonNode root = worldBankRestClient.get()
                .uri(uri -> uri.path("/country")
                        .queryParam("region", "SSF")
                        .queryParam("format", "json")
                        .queryParam("per_page", 300)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        if (root == null || !root.isArray() || root.size() < 2) {
            throw new IllegalStateException("Unexpected response shape from the World Bank API");
        }

        // 2. The countries live in the SECOND element of the array.
        JsonNode countries = root.get(1);

        // 3. Save the ones we don't already have.
        int fetched = 0;
        int created = 0;
        int skipped = 0;
        for (JsonNode country : countries) {
            fetched++;
            String iso = textOrNull(country.get("id"));
            String name = textOrNull(country.get("name"));

            // Skip aggregates/placeholders: real countries have a 3-letter id and a name.
            if (iso == null || iso.length() != 3 || name == null || name.isBlank()) {
                skipped++;
                continue;
            }
            if (recipientRepository.existsByIsoCodeIgnoreCase(iso)
                    || recipientRepository.existsByCountryNameIgnoreCase(name)) {
                skipped++;
                continue;
            }

            JsonNode regionNode = country.get("region");
            String region = (regionNode == null) ? null : textOrNull(regionNode.get("value"));

            recipientRepository.save(Recipient.builder()
                    .countryName(name)
                    .isoCode(iso.toUpperCase())
                    .region(region == null ? null : region.trim())
                    .build());
            created++;
        }

        log.info("World Bank recipient import: fetched={}, created={}, skipped={}", fetched, created, skipped);
        return new IngestionResult(fetched, created, skipped);
    }

    /** Returns the node's text value, or null if the node is absent/null. */
    private String textOrNull(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText();
    }
}
