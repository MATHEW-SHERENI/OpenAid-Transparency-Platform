package com.mathewshereni.open_aid_transparency.sdg;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One of the 17 UN Sustainable Development Goals (SDGs).
 * This is "reference data": a small, fixed list that aid projects link to.
 */
@Entity                       // "Map this class to a database table."
@Table(name = "sdg_goals")    // Explicit table name (good habit; avoids surprises).
@Getter                       // Lombok generates getId(), getGoalNumber(), ... at compile time.
@Setter                       // Lombok generates setId(...), setGoalNumber(...), ...
@NoArgsConstructor            // JPA REQUIRES a no-argument constructor to build objects from rows.
@AllArgsConstructor           // Convenience constructor with every field (used by @Builder).
@Builder                      // Lets us write SdgGoal.builder().goalNumber(1).title("...").build()
public class SdgGoal {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "goal_number", nullable = false, unique = true)
    private Integer goalNumber;


    @Column(nullable = false, length = 150)
    private String title;


    @Column(length = 1000)
    private String description;
}
