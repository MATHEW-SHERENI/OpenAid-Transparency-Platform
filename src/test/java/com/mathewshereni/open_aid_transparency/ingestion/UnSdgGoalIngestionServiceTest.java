package com.mathewshereni.open_aid_transparency.ingestion;

import com.mathewshereni.open_aid_transparency.sdg.SdgGoal;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the UN SDG goal import. Missing goals are created; existing ones
 * keep their (concise) title but get the authoritative UN description.
 */
@ExtendWith(MockitoExtension.class)
class UnSdgGoalIngestionServiceTest {

    @Mock private UnSdgClient client;
    @Mock private SdgGoalRepository repository;

    @InjectMocks
    private UnSdgGoalIngestionService service;

    @Test
    void createsMissingGoalsAndEnrichesExistingOnes() {
        when(client.fetchGoals()).thenReturn(List.of(
                new UnSdgGoal(1, "End poverty in all its forms everywhere", "Goal 1 description"),
                new UnSdgGoal(6, "Clean water and sanitation for all", "Goal 6 description")));

        // Goal 1 is new; goal 6 already exists with our short display title.
        when(repository.findByGoalNumber(1)).thenReturn(Optional.empty());
        SdgGoal existingGoal6 = SdgGoal.builder()
                .id(6L).goalNumber(6).title("Clean Water and Sanitation").build();
        when(repository.findByGoalNumber(6)).thenReturn(Optional.of(existingGoal6));

        IngestionResult result = service.importGoals();

        assertThat(result.fetched()).isEqualTo(2);
        assertThat(result.created()).isEqualTo(1);    // goal 1
        assertThat(result.skipped()).isEqualTo(1);    // goal 6 refreshed

        verify(repository, times(2)).save(org.mockito.ArgumentMatchers.any(SdgGoal.class));
        // Existing goal keeps its short title but gains the UN description.
        assertThat(existingGoal6.getTitle()).isEqualTo("Clean Water and Sanitation");
        assertThat(existingGoal6.getDescription()).isEqualTo("Goal 6 description");
    }
}
