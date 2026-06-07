package com.mathewshereni.open_aid_transparency.feedback;

import com.mathewshereni.open_aid_transparency.project.AidProject;
import com.mathewshereni.open_aid_transparency.project.ProjectStatus;
import com.mathewshereni.open_aid_transparency.user.AppUser;
import com.mathewshereni.open_aid_transparency.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice tests for FeedbackRepository. The custom query both FILTERS by
 * project and ORDERS newest-first, so we verify both: only one project's feedback
 * comes back, and it is sorted by createdAt descending.
 */
@DataJpaTest
class FeedbackRepositoryTest {

    @Autowired
    private FeedbackRepository repository;

    @Autowired
    private TestEntityManager em;

    private AidProject persistProject(String title) {
        return em.persist(AidProject.builder().title(title).status(ProjectStatus.ACTIVE).build());
    }

    private AppUser persistAuthor() {
        return em.persist(AppUser.builder()
                .username("amina").email("amina@x.io")
                .passwordHash("hash").role(UserRole.CITIZEN).enabled(true)
                .build());
    }

    /** Persist + flush so @CreationTimestamp is assigned now; pause so timestamps differ. */
    private Feedback persistFeedback(AidProject project, AppUser author, int rating) throws InterruptedException {
        Feedback fb = em.persist(Feedback.builder()
                .project(project).author(author).rating(rating).build());
        em.flush();
        Thread.sleep(10);
        return fb;
    }

    @Test
    void findByProjectIdOrderByCreatedAtDesc_filtersByProjectAndReturnsNewestFirst() throws InterruptedException {
        AppUser author = persistAuthor();
        AidProject target = persistProject("Clean Water");
        AidProject other = persistProject("Road Repair");

        // Two feedbacks on the target project (oldest first), one on another project.
        persistFeedback(target, author, 3);   // oldest for target
        persistFeedback(other, author, 1);    // belongs to a different project
        persistFeedback(target, author, 5);   // newest for target
        em.clear();

        List<Feedback> result = repository.findByProject_IdOrderByCreatedAtDesc(target.getId());

        // Only the target project's two feedbacks, newest (rating 5) first.
        assertThat(result).hasSize(2)
                .extracting(Feedback::getRating)
                .containsExactly(5, 3);
    }

    @Test
    void findByProjectIdOrderByCreatedAtDesc_returnsEmptyForProjectWithNoFeedback() {
        AidProject lonely = persistProject("No Feedback Yet");
        em.flush();

        assertThat(repository.findByProject_IdOrderByCreatedAtDesc(lonely.getId())).isEmpty();
    }
}
