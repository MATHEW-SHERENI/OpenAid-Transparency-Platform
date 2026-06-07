package com.mathewshereni.open_aid_transparency.ingestion;

import java.math.BigDecimal;

/**
 * One raw data point from the World Bank "net ODA received" indicator: how much
 * official development assistance a country received in a given year (current US$).
 *
 * This is the "item" that flows through the Spring Batch pipeline - the reader
 * produces these, the processor turns each into a FundingFlow (or discards it).
 */
public record WorldBankOdaPoint(
        String iso,
        String countryName,
        int year,
        BigDecimal amountUsd
) {
}
