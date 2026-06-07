package com.mathewshereni.open_aid_transparency.recipient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Input shape for creating/updating a recipient country.
 * isoCode is optional, but IF supplied must be exactly 3 letters (ISO 3166
 * alpha-3). @Pattern is skipped automatically when the value is null.
 */
public record RecipientRequest(

        @NotBlank(message = "countryName is required")
        @Size(max = 100, message = "countryName must be at most 100 characters")
        String countryName,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "isoCode must be exactly 3 letters")
        String isoCode,

        @Size(max = 100, message = "region must be at most 100 characters")
        String region
) {
}
