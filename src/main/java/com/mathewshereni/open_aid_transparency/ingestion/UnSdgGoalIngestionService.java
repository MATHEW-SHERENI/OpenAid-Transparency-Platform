package com.mathewshereni.open_aid_transparency.ingestion;

import com.mathewshereni.open_aid_transparency.sdg.SdgGoal;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Imports the SDG goal taxonomy from the UN SDG API, upserting our sdg_goals
 * table. This replaces the hardcoded SdgGoalDataInitializer seed with the
 * authoritative source: existing goals are ENRICHED with the UN description
 * (which the seed left blank), while keeping our concise display titles; any
 * missing goal is created.
 *
 * In the returned IngestionResult: fetched = goals from the API, created = new
 * rows, skipped = goals that already existed (their description was refreshed).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UnSdgGoalIngestionService {

    private final UnSdgClient client;
    private final SdgGoalRepository repository;

    @Transactional
    public IngestionResult importGoals() {
        List<UnSdgGoal> goals = client.fetchGoals();

        int fetched = 0;
        int created = 0;
        int refreshed = 0;
        for (UnSdgGoal goal : goals) {
            fetched++;
            SdgGoal existing = repository.findByGoalNumber(goal.number()).orElse(null);
            if (existing == null) {
                repository.save(SdgGoal.builder()
                        .goalNumber(goal.number())
                        .title(goal.title())
                        .description(goal.description())
                        .build());
                created++;
            } else {
                // Keep our short title; fill in the authoritative description.
                existing.setDescription(goal.description());
                repository.save(existing);
                refreshed++;
            }
        }

        IngestionResult result = new IngestionResult(fetched, created, refreshed);
        log.info("UN SDG goal import: {}", result);
        return result;
    }
}
