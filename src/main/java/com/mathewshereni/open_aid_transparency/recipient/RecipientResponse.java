package com.mathewshereni.open_aid_transparency.recipient;

public record RecipientResponse(
        Long id,
        String countryName,
        String isoCode,
        String region
) {
}
