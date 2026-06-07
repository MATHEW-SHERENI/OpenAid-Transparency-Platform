package com.mathewshereni.open_aid_transparency.sdg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SdgGoalService. This is a read-only reference-data service, so
 * the behaviour to pin down is the lookup-by-number path: it maps to a DTO when
 * found, and throws a 404 ResponseStatusException when not.
 */
@ExtendWith(MockitoExtension.class)
class SdgGoalServiceTest {

    @Mock
    private SdgGoalRepository repository;

    @InjectMocks
    private SdgGoalService service;

    private SdgGoal goal(int number, String title) {
        return SdgGoal.builder().id((long) number).goalNumber(number).title(title)
                .description("desc").build();
    }

    @Test
    void findByGoalNumber_returnsDtoWhenFound() {
        when(repository.findByGoalNumber(6)).thenReturn(Optional.of(goal(6, "Clean Water")));

        SdgGoalDto dto = service.findByGoalNumber(6);

        assertThat(dto.goalNumber()).isEqualTo(6);
        assertThat(dto.title()).isEqualTo("Clean Water");
    }

    @Test
    void findByGoalNumber_throws404WhenMissing() {
        when(repository.findByGoalNumber(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByGoalNumber(99))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("99")
                // ResponseStatusException carries the HTTP status; confirm it's 404.
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void findAll_mapsEveryGoalToDto() {
        when(repository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(goal(1, "No Poverty"), goal(2, "Zero Hunger")));

        List<SdgGoalDto> dtos = service.findAll();

        assertThat(dtos).extracting(SdgGoalDto::goalNumber).containsExactly(1, 2);
    }
}
