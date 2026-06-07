package com.mathewshereni.open_aid_transparency.project;

import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.recipient.RecipientRepository;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoal;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AidProjectService. The logic worth locking down:
 *   - the date rule (endDate must not precede startDate) - a business rule that
 *     cannot be expressed with a field annotation,
 *   - turning SDG goal NUMBERS into goal ENTITIES, rejecting unknown numbers,
 *   - flattening the recipient + the many-to-many goals into the response, with
 *     goals sorted by number.
 */
@ExtendWith(MockitoExtension.class)
class AidProjectServiceTest {

    @Mock private AidProjectRepository repository;
    @Mock private RecipientRepository recipientRepository;
    @Mock private SdgGoalRepository sdgGoalRepository;

    @InjectMocks
    private AidProjectService service;

    private Recipient recipient() {
        return Recipient.builder().id(2L).countryName("Kenya").isoCode("KEN").build();
    }

    private SdgGoal goal(long id, int number, String title) {
        return SdgGoal.builder().id(id).goalNumber(number).title(title).build();
    }

    private AidProjectRequest request(LocalDate start, LocalDate end, Set<Integer> goals) {
        return new AidProjectRequest("Clean Water", "desc", ProjectStatus.ACTIVE, start, end, 2L, goals);
    }

    @Test
    void create_resolvesGoalsAndReturnsThemSortedByNumber() {
        when(recipientRepository.findById(2L)).thenReturn(Optional.of(recipient()));
        // Return goals out of order to prove the service sorts them in the response.
        when(sdgGoalRepository.findByGoalNumberIn(Set.of(6, 13)))
                .thenReturn(List.of(goal(13L, 13, "Climate Action"), goal(6L, 6, "Clean Water")));
        when(repository.save(any(AidProject.class))).thenAnswer(inv -> {
            AidProject p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        AidProjectResponse response = service.create(
                request(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), Set.of(6, 13)));

        assertThat(response.recipient().countryName()).isEqualTo("Kenya");
        assertThat(response.sdgGoals())
                .extracting(AidProjectResponse.SdgGoalSummary::goalNumber)
                .containsExactly(6, 13);   // sorted ascending regardless of repo order
    }

    @Test
    void create_rejectsEndDateBeforeStartDate() {
        // Validation happens before any repository work, so no stubs are needed.
        assertThatThrownBy(() -> service.create(
                request(LocalDate.of(2024, 12, 31), LocalDate.of(2024, 1, 1), Set.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate");
        verify(repository, never()).save(any());
    }

    @Test
    void create_allowsNullDates() {
        when(recipientRepository.findById(2L)).thenReturn(Optional.of(recipient()));
        when(repository.save(any(AidProject.class))).thenAnswer(inv -> inv.getArgument(0));

        AidProjectResponse response = service.create(request(null, null, null));

        assertThat(response.startDate()).isNull();
        assertThat(response.sdgGoals()).isEmpty();
        // Null goal numbers must not hit the goal repository at all.
        verify(sdgGoalRepository, never()).findByGoalNumberIn(anySet());
    }

    @Test
    void create_rejectsUnknownSdgGoalNumber() {
        when(recipientRepository.findById(2L)).thenReturn(Optional.of(recipient()));
        // Asked for {6, 99}, but only 6 exists -> 99 is unknown.
        when(sdgGoalRepository.findByGoalNumberIn(Set.of(6, 99)))
                .thenReturn(List.of(goal(6L, 6, "Clean Water")));

        assertThatThrownBy(() -> service.create(
                request(null, null, Set.of(6, 99))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(repository, never()).save(any());
    }

    @Test
    void create_throwsWhenRecipientMissing() {
        when(recipientRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(null, null, Set.of())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipient 2");
        verify(repository, never()).save(any());
    }

    @Test
    void update_replacesGoalLinks() {
        AidProject existing = AidProject.builder()
                .id(1L).title("Old").status(ProjectStatus.PLANNED).recipient(recipient())
                .build();
        existing.getSdgGoals().add(goal(1L, 1, "No Poverty"));   // a stale link to be replaced

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(recipientRepository.findById(2L)).thenReturn(Optional.of(recipient()));
        when(sdgGoalRepository.findByGoalNumberIn(Set.of(6)))
                .thenReturn(List.of(goal(6L, 6, "Clean Water")));
        when(repository.save(any(AidProject.class))).thenAnswer(inv -> inv.getArgument(0));

        AidProjectResponse response = service.update(1L,
                request(null, null, Set.of(6)));

        // Old goal (1) gone, new goal (6) present.
        assertThat(response.sdgGoals())
                .extracting(AidProjectResponse.SdgGoalSummary::goalNumber)
                .containsExactly(6);

        // The same managed entity should have been saved (set cleared + refilled in place).
        ArgumentCaptor<AidProject> saved = ArgumentCaptor.forClass(AidProject.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSdgGoals()).hasSize(1);
    }

    @Test
    void findById_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Aid project 404");
    }

    @Test
    void delete_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
