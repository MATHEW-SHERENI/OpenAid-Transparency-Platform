package com.mathewshereni.open_aid_transparency.sdg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;


 // Seeds the 17 UN Sustainable Development Goals into the database on startup.

@Component
@Slf4j                     // Lombok gives us a logger named 'log'.
@RequiredArgsConstructor
public class SdgGoalDataInitializer implements CommandLineRunner {

    private final SdgGoalRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("SDG goals already present ({}), skipping seed.", repository.count());
            return;
        }

        List<SdgGoal> goals = List.of(
                goal(1, "No Poverty"),
                goal(2, "Zero Hunger"),
                goal(3, "Good Health and Well-being"),
                goal(4, "Quality Education"),
                goal(5, "Gender Equality"),
                goal(6, "Clean Water and Sanitation"),
                goal(7, "Affordable and Clean Energy"),
                goal(8, "Decent Work and Economic Growth"),
                goal(9, "Industry, Innovation and Infrastructure"),
                goal(10, "Reduced Inequalities"),
                goal(11, "Sustainable Cities and Communities"),
                goal(12, "Responsible Consumption and Production"),
                goal(13, "Climate Action"),
                goal(14, "Life Below Water"),
                goal(15, "Life on Land"),
                goal(16, "Peace, Justice and Strong Institutions"),
                goal(17, "Partnerships for the Goals")
        );

        repository.saveAll(goals);
        log.info("Seeded {} SDG goals.", goals.size());
    }

    private SdgGoal goal(int number, String title) {
        return SdgGoal.builder()
                .goalNumber(number)
                .title(title)
                .build();
    }
}
