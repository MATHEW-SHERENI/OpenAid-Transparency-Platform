package com.mathewshereni.open_aid_transparency.feedback;

import java.time.Instant;

public record FeedbackResponse(
        Long id,
        Integer rating,
        String comment,
        Instant createdAt,
        ProjectSummary project,
        AuthorSummary author
) {
    public record ProjectSummary(Long id, String title) {
    }

    public record AuthorSummary(Long id, String username) {
    }
}
