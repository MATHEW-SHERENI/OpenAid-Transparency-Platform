package com.mathewshereni.open_aid_transparency.funding;

import java.math.BigDecimal;

/**
 * One row of the "total funding per year" trend report. Amounts are summed across
 * currencies (the dataset is USD); group by year only.
 */
public record FundingByYear(
        Integer year,
        BigDecimal totalAmount,
        Long flowCount
) {
}
