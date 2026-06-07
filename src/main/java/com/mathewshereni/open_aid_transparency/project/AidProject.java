package com.mathewshereni.open_aid_transparency.project;

import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A funded aid initiative carried out in one recipient country, advancing one
 * or more Sustainable Development Goals.
 */
@Entity
@Table(name = "aid_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AidProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * RELATIONSHIP 1 - Many-to-One.
     * Many projects belong to one recipient country.
     *
     * @JoinColumn names the foreign-key column on THIS table -> "recipient_id".
     * fetch = LAZY: don't load the Recipient from the DB until we actually call
     * getRecipient(); this keeps project queries cheap.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private Recipient recipient;

    /**
     * RELATIONSHIP 2 - Many-to-Many.
     * A project advances many SDGs; an SDG is advanced by many projects.
     *
     * @JoinTable tells Hibernate to create the in-between "link" table and what
     * to call its two pointer columns. We store the links in a Set because a
     * project should never list the same goal twice.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "aid_project_sdg_goals",
            joinColumns = @JoinColumn(name = "aid_project_id"),
            inverseJoinColumns = @JoinColumn(name = "sdg_goal_id")
    )
    @Builder.Default
    private Set<SdgGoal> sdgGoals = new LinkedHashSet<>();
}
