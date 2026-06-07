package com.mathewshereni.open_aid_transparency.funding;

import java.math.BigDecimal;

/**
 * One row of the "total funding per recipient country" report.
 *
 * We group by currency as well, because summing different currencies together
 * would be meaningless (you can't add 100 USD to 100 EUR).
 */
public record FundingByRecipient(
        Long recipientId,
        String countryName,
        String currency,
        BigDecimal totalAmount,
        Long flowCount
) {
}
