package com.mathewshereni.open_aid_transparency.sdg;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice tests for SdgGoalRepository. findByGoalNumber backs single
 * lookups; findByGoalNumberIn backs resolving a project's set of goal numbers.
 */
@DataJpaTest
class SdgGoalRepositoryTest {

    @Autowired
    private SdgGoalRepository repository;

    @Autowired
    private TestEntityManager em;

    private void persistGoal(int number, String title) {
        em.persist(SdgGoal.builder().goalNumber(number).title(title).build());
    }

    @Test
    void findByGoalNumber_returnsMatchOrEmpty() {
        persistGoal(6, "Clean Water");
        em.flush();

        assertThat(repository.findByGoalNumber(6)).isPresent();
        assertThat(repository.findByGoalNumber(99)).isEmpty();
    }

    @Test
    void findByGoalNumberIn_returnsOnlyTheRequestedGoals() {
        persistGoal(1, "No Poverty");
        persistGoal(6, "Clean Water");
        persistGoal(13, "Climate Action");
        em.flush();

        List<SdgGoal> found = repository.findByGoalNumberIn(Set.of(6, 13));

        assertThat(found).hasSize(2)
                .extracting(SdgGoal::getGoalNumber)
                .containsExactlyInAnyOrder(6, 13);
    }

    @Test
    void findByGoalNumberIn_returnsEmptyWhenNoneMatch() {
        persistGoal(6, "Clean Water");
        em.flush();

        assertThat(repository.findByGoalNumberIn(Set.of(98, 99))).isEmpty();
    }
}
