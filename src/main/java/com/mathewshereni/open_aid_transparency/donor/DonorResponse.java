package com.mathewshereni.open_aid_transparency.donor;

/**
 * What we RETURN to clients for a donor. (Output shape.)
 */
public record DonorResponse(
        Long id,
        String name,
        DonorType type,
        String country
) {
}
