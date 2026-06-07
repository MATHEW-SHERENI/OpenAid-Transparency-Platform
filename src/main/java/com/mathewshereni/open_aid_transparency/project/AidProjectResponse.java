package com.mathewshereni.open_aid_transparency.project;

import java.time.LocalDate;
import java.util.List;

/**
 * Output shape for a project. Relationships are flattened into small, readable
 * "summary" records - we expose just enough of the related data, never the raw
 * JPA entities (which could trigger lazy-loading surprises in JSON).
 */
public record AidProjectResponse(
        Long id,
        String title,
        String description,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate,
        RecipientSummary recipient,
        List<SdgGoalSummary> sdgGoals
) {
    public record RecipientSummary(Long id, String countryName) {
    }

    public record SdgGoalSummary(Integer goalNumber, String title) {
    }
}
