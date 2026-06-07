package com.mathewshereni.open_aid_transparency.sdg;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Business-logic layer for SDG goals. The controller calls this; this calls the
 * repository. Keeping logic here (not in the controller) is what lets the same
 * rules be reused by other callers later (e.g. batch jobs, reports).
 */
@Service
@RequiredArgsConstructor   // Lombok generates a constructor for the final fields below.
public class SdgGoalService {

    /**
     * "Dependency injection": we declare what we need (the repository) as a final
     * field, and Spring passes it into the constructor for us automatically.
     */
    private final SdgGoalRepository repository;

    /** Return all goals as DTOs, ordered by goal number 1..17. */
    public List<SdgGoalDto> findAll() {
        return repository.findAll(Sort.by("goalNumber"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** Return one goal by its number, or a 404 if it doesn't exist. */
    public SdgGoalDto findByGoalNumber(Integer goalNumber) {
        return repository.findByGoalNumber(goalNumber)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SDG goal " + goalNumber + " not found"));
    }

    /** Convert a database entity into the web-facing DTO. */
    private SdgGoalDto toDto(SdgGoal goal) {
        return new SdgGoalDto(
                goal.getId(),
                goal.getGoalNumber(),
                goal.getTitle(),
                goal.getDescription());
    }
}
