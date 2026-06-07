package com.mathewshereni.open_aid_transparency.sdg;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for SdgGoal.
 *
 * By extending JpaRepository<SdgGoal, Long> we INHERIT a full set of methods
 * for free - save(), findAll(), findById(), count(), delete(), and many more.
 * We never write their implementation; Spring Data generates it at runtime.
 *
 * The two methods below are "derived queries": Spring reads the METHOD NAME and
 * writes the SQL for us. findByGoalNumber -> "WHERE goal_number = ?".
 */
public interface SdgGoalRepository extends JpaRepository<SdgGoal, Long> {

    Optional<SdgGoal> findByGoalNumber(Integer goalNumber);

    /** Fetch many goals at once by their numbers - used to resolve a project's SDGs. */
    List<SdgGoal> findByGoalNumberIn(Collection<Integer> goalNumbers);
}
