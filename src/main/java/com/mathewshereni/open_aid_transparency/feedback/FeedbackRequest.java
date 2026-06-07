package com.mathewshereni.open_aid_transparency.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Input shape for submitting feedback. There is deliberately NO authorId: the
 * author is taken from the authenticated user (their JWT), never from the client.
 */
public record FeedbackRequest(

        @NotNull(message = "projectId is required")
        Long projectId,

        @NotNull(message = "rating is required")
        @Min(value = 1, message = "rating must be between 1 and 5")
        @Max(value = 5, message = "rating must be between 1 and 5")
        Integer rating,

        @Size(max = 2000, message = "comment must be at most 2000 characters")
        String comment
) {
}
