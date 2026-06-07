package com.mathewshereni.open_aid_transparency.funding;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Output shape for a funding flow, with the three related entities flattened
 * into small summaries. The project summary is null when the flow isn't tied
 * to a specific project.
 */
public record FundingFlowResponse(
        Long id,
        BigDecimal amount,
        String currency,
        Integer year,
        LocalDate transactionDate,
        DonorSummary donor,
        RecipientSummary recipient,
        ProjectSummary project
) {
    public record DonorSummary(Long id, String name) {
    }

    public record RecipientSummary(Long id, String countryName) {
    }

    public record ProjectSummary(Long id, String title) {
    }
}
