package com.mathewshereni.open_aid_transparency.feedback;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService service;

    /**
     * GET /api/feedback                 -> all feedback
     * GET /api/feedback?projectId=1     -> only feedback for project 1
     * The optional query parameter lets one endpoint serve both needs.
     */
    @GetMapping
    public List<FeedbackResponse> getAll(@RequestParam(required = false) Long projectId) {
        return (projectId == null) ? service.findAll() : service.findByProject(projectId);
    }

    @GetMapping("/{id}")
    public FeedbackResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    /**
     * Spring injects the current Authentication (set by our JwtAuthenticationFilter).
     * authentication.getName() is the logged-in username - that's who the feedback
     * is from. The client cannot spoof it.
     */
    @PostMapping
    public ResponseEntity<FeedbackResponse> create(@Valid @RequestBody FeedbackRequest request,
                                                   Authentication authentication,
                                                   UriComponentsBuilder uriBuilder) {
        FeedbackResponse created = service.create(request, authentication.getName());
        URI location = uriBuilder.path("/api/feedback/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
