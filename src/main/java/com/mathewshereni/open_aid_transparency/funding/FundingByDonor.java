package com.mathewshereni.open_aid_transparency.funding;

import java.math.BigDecimal;

/**
 * One row of the "total funding per donor" report - who has given the most.
 */
public record FundingByDonor(
        Long donorId,
        String donorName,
        BigDecimal totalAmount,
        Long flowCount
) {
}
