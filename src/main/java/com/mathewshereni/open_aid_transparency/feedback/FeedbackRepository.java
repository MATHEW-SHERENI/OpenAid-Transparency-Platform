package com.mathewshereni.open_aid_transparency.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /**
     * All feedback for one project, newest first. The underscore in
     * "Project_Id" tells Spring Data to navigate feedback.project.id.
     */
    List<Feedback> findByProject_IdOrderByCreatedAtDesc(Long projectId);
}
