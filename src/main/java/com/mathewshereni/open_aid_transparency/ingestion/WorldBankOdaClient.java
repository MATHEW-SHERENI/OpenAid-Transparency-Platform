package com.mathewshereni.open_aid_transparency.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Talks to the World Bank indicator API and returns clean ODA data points.
 *
 * This class owns the messy parts - the URL, the [metadata, data] envelope, and
 * PAGINATION (the API returns at most per_page rows, telling us the total page
 * count in the metadata). Keeping all that here means the Spring Batch reader can
 * stay trivial, and this logic is independently understandable/testable.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WorldBankOdaClient {

    /** Net official development assistance received (current US$). */
    private static final String INDICATOR = "DT.ODA.ODAT.CD";
    private static final int PER_PAGE = 1000;

    private final RestClient worldBankRestClient;

    /**
     * Fetch every ODA data point (with a non-null value) for all countries between
     * the two years inclusive. We fetch ALL countries here and let the processor
     * keep only the ones we actually track - simpler than per-country calls.
     */
    public List<WorldBankOdaPoint> fetchOdaReceived(int fromYear, int toYear) {
        List<WorldBankOdaPoint> points = new ArrayList<>();
        int page = 1;
        int totalPages = 1;

        do {
            final int currentPage = page;
            JsonNode root = worldBankRestClient.get()
                    .uri(uri -> uri.path("/country/all/indicator/" + INDICATOR)
                            .queryParam("format", "json")
                            .queryParam("date", fromYear + ":" + toYear)
                            .queryParam("per_page", PER_PAGE)
                            .queryParam("page", currentPage)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            // The body is [ {metadata}, [ {dataPoint}, ... ] ].
            if (root == null || !root.isArray() || root.size() < 2) {
                break;
            }
            totalPages = intOrDefault(root.get(0).get("pages"), 1);

            for (JsonNode node : root.get(1)) {
                JsonNode valueNode = node.get("value");
                if (valueNode == null || valueNode.isNull() || !valueNode.isNumber()) {
                    continue;   // no reported figure for this country/year
                }
                String iso = textOrNull(node.get("countryiso3code"));
                String dateStr = textOrNull(node.get("date"));
                if (iso == null || iso.length() != 3 || dateStr == null) {
                    continue;   // aggregates/regions have non-3-letter codes
                }
                points.add(new WorldBankOdaPoint(
                        iso.toUpperCase(),
                        countryName(node),
                        Integer.parseInt(dateStr),
                        valueNode.decimalValue()));
            }
            page++;
        } while (page <= totalPages);

        log.info("World Bank ODA fetch {}-{}: {} data points with values", fromYear, toYear, points.size());
        return points;
    }

    private String countryName(JsonNode dataPoint) {
        JsonNode country = dataPoint.get("country");
        return (country == null) ? null : textOrNull(country.get("value"));
    }

    private String textOrNull(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText();
    }

    private int intOrDefault(JsonNode node, int fallback) {
        return (node == null || !node.isNumber()) ? fallback : node.intValue();
    }
}
