package com.mathewshereni.open_aid_transparency.feedback;

import com.mathewshereni.open_aid_transparency.project.AidProject;
import com.mathewshereni.open_aid_transparency.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A citizen's assessment of an aid project's real-world outcome - the validation
 * loop that makes the platform "transparent" rather than just a data dump.
 */
@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Which project this feedback is about. Required.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aid_project_id", nullable = false)
    private AidProject project;

    /**
     * Who wrote the feedback. Required.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    /**
     * A 1-5 star rating. We enforce the 1..5 range in the service layer later.
     */
    @Column(nullable = false)
    private Integer rating;

    /**
     * Free-text comment. Optional, up to 2000 characters.
     */
    @Column(length = 2000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
