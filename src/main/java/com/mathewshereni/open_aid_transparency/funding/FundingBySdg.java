package com.mathewshereni.open_aid_transparency.funding;

import java.math.BigDecimal;

/**
 * One slice of the "funding by SDG" report: how much funding is tagged to a given
 * Sustainable Development Goal (the category, e.g. "Clean Water and Sanitation").
 * Flows with no SDG tag are excluded from this report.
 */
public record FundingBySdg(
        Integer goalNumber,
        String goalTitle,
        BigDecimal totalAmount,
        Long flowCount
) {
}
