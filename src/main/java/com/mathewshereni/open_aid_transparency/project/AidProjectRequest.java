package com.mathewshereni.open_aid_transparency.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

/**
 * Input shape for creating/updating a project. Note the relationships arrive as
 * plain IDs/numbers - the client never sends nested Recipient or SdgGoal objects.
 * The service resolves these into real entities (or 404s if they don't exist).
 */
public record AidProjectRequest(

        @NotBlank(message = "title is required")
        @Size(max = 250, message = "title must be at most 250 characters")
        String title,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @NotNull(message = "status is required")
        ProjectStatus status,

        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "recipientId is required")
        Long recipientId,

        /** SDG goal numbers (1..17) this project advances. May be empty/omitted. */
        Set<Integer> sdgGoalNumbers
) {
}
