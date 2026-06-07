package com.mathewshereni.open_aid_transparency.project;

import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.recipient.RecipientRepository;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoal;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for aid projects. This service depends on THREE repositories
 * because a project ties together its own table plus Recipient and SdgGoal.
 */
@Service
@RequiredArgsConstructor
public class AidProjectService {

    private final AidProjectRepository repository;
    private final RecipientRepository recipientRepository;
    private final SdgGoalRepository sdgGoalRepository;

    @Transactional(readOnly = true)
    public List<AidProjectResponse> findAll() {
        return repository.findAll(Sort.by("title"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AidProjectResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public AidProjectResponse create(AidProjectRequest request) {
        validateDates(request);
        AidProject project = AidProject.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .recipient(resolveRecipient(request.recipientId()))
                .sdgGoals(resolveGoals(request.sdgGoalNumbers()))
                .build();
        return toResponse(repository.save(project));
    }

    @Transactional
    public AidProjectResponse update(Long id, AidProjectRequest request) {
        validateDates(request);
        AidProject project = getOrThrow(id);
        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setStatus(request.status());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setRecipient(resolveRecipient(request.recipientId()));
        // Replace the goal links: clear the existing set, then add the resolved ones.
        project.getSdgGoals().clear();
        project.getSdgGoals().addAll(resolveGoals(request.sdgGoalNumbers()));
        return toResponse(repository.save(project));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    // ---------- helpers ----------

    /** Business rule (not expressible as a field annotation): end can't precede start. */
    private void validateDates(AidProjectRequest r) {
        if (r.startDate() != null && r.endDate() != null && r.endDate().isBefore(r.startDate())) {
            throw new IllegalArgumentException("endDate must not be before startDate.");
        }
    }

    /** Turn a recipientId into a managed Recipient entity, or 404. */
    private Recipient resolveRecipient(Long recipientId) {
        return recipientRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient " + recipientId + " not found."));
    }

    /** Turn a set of goal numbers into SdgGoal entities; 404 if any number is unknown. */
    private Set<SdgGoal> resolveGoals(Set<Integer> goalNumbers) {
        if (goalNumbers == null || goalNumbers.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<SdgGoal> found = sdgGoalRepository.findByGoalNumberIn(goalNumbers);
        if (found.size() != goalNumbers.size()) {
            Set<Integer> foundNumbers = found.stream()
                    .map(SdgGoal::getGoalNumber)
                    .collect(Collectors.toSet());
            List<Integer> missing = goalNumbers.stream()
                    .filter(n -> !foundNumbers.contains(n))
                    .sorted()
                    .toList();
            throw new ResourceNotFoundException("Unknown SDG goal number(s): " + missing);
        }
        return new LinkedHashSet<>(found);
    }

    private AidProject getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aid project " + id + " not found."));
    }

    private AidProjectResponse toResponse(AidProject p) {
        AidProjectResponse.RecipientSummary recipient = new AidProjectResponse.RecipientSummary(
                p.getRecipient().getId(),
                p.getRecipient().getCountryName());

        List<AidProjectResponse.SdgGoalSummary> goals = p.getSdgGoals().stream()
                .sorted(Comparator.comparing(SdgGoal::getGoalNumber))
                .map(g -> new AidProjectResponse.SdgGoalSummary(g.getGoalNumber(), g.getTitle()))
                .toList();

        return new AidProjectResponse(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getStatus(),
                p.getStartDate(),
                p.getEndDate(),
                recipient,
                goals);
    }
}
