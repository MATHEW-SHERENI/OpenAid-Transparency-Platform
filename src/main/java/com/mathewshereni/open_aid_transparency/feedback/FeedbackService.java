package com.mathewshereni.open_aid_transparency.feedback;

import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import com.mathewshereni.open_aid_transparency.project.AidProject;
import com.mathewshereni.open_aid_transparency.project.AidProjectRepository;
import com.mathewshereni.open_aid_transparency.user.AppUser;
import com.mathewshereni.open_aid_transparency.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository repository;
    private final AidProjectRepository projectRepository;
    private final AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FeedbackResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> findByProject(Long projectId) {
        return repository.findByProject_IdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public FeedbackResponse create(FeedbackRequest request, String authorUsername) {
        AidProject project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Aid project " + request.projectId() + " not found."));
        // The author is the authenticated user, resolved from their token's username.
        AppUser author = userRepository.findByUsernameIgnoreCase(authorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User '" + authorUsername + "' not found."));

        Feedback feedback = Feedback.builder()
                .project(project)
                .author(author)
                .rating(request.rating())
                .comment(request.comment())
                .build();

        return toResponse(repository.save(feedback));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Feedback getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback " + id + " not found."));
    }

    private FeedbackResponse toResponse(Feedback f) {
        return new FeedbackResponse(
                f.getId(),
                f.getRating(),
                f.getComment(),
                f.getCreatedAt(),
                new FeedbackResponse.ProjectSummary(f.getProject().getId(), f.getProject().getTitle()),
                new FeedbackResponse.AuthorSummary(f.getAuthor().getId(), f.getAuthor().getUsername()));
    }
}
