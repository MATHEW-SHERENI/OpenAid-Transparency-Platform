package com.mathewshereni.open_aid_transparency.funding;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input shape for a funding flow. donorId and recipientId are required;
 * projectId is optional (some aid is general budget support, not tied to a project).
 */
public record FundingFlowRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter code")
        String currency,

        @NotNull(message = "year is required")
        @Min(value = 1900, message = "year must be 1900 or later")
        @Max(value = 2100, message = "year must be 2100 or earlier")
        Integer year,

        LocalDate transactionDate,

        @NotNull(message = "donorId is required")
        Long donorId,

        @NotNull(message = "recipientId is required")
        Long recipientId,

        Long projectId
) {
}
