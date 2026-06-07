package com.mathewshereni.open_aid_transparency.sdg;


 // Data Transfer Object - the shape of an SDG goal that we expose over the web.

public record SdgGoalDto(
        Long id,
        Integer goalNumber,
        String title,
        String description
) {
}
