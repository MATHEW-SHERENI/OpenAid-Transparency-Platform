package com.mathewshereni.open_aid_transparency.ingestion;

/**
 * One Sustainable Development Goal as returned by the UN SDG API
 * (https://unstats.un.org/sdgapi/v1/sdg/Goal/List).
 */
public record UnSdgGoal(
        int number,
        String title,
        String description
) {
}
