package com.mathewshereni.open_aid_transparency.feedback;

import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import com.mathewshereni.open_aid_transparency.project.AidProject;
import com.mathewshereni.open_aid_transparency.project.AidProjectRepository;
import com.mathewshereni.open_aid_transparency.project.ProjectStatus;
import com.mathewshereni.open_aid_transparency.user.AppUser;
import com.mathewshereni.open_aid_transparency.user.AppUserRepository;
import com.mathewshereni.open_aid_transparency.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FeedbackService. The interesting behaviour here is that the
 * author is NOT taken from the request - it is the authenticated username passed
 * in separately, then resolved to a real AppUser (or 404). We also confirm the
 * project + author are flattened into summaries on the response.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private FeedbackRepository repository;
    @Mock private AidProjectRepository projectRepository;
    @Mock private AppUserRepository userRepository;

    @InjectMocks
    private FeedbackService service;

    private AidProject project() {
        return AidProject.builder().id(5L).title("Clean Water").status(ProjectStatus.ACTIVE).build();
    }

    private AppUser author() {
        return AppUser.builder().id(3L).username("amina").email("amina@x.io").role(UserRole.CITIZEN).build();
    }

    @Test
    void create_resolvesProjectAndAuthenticatedAuthorThenFlattensThem() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project()));
        when(userRepository.findByUsernameIgnoreCase("amina")).thenReturn(Optional.of(author()));
        when(repository.save(any(Feedback.class))).thenAnswer(inv -> {
            Feedback f = inv.getArgument(0);
            f.setId(100L);
            return f;
        });

        FeedbackResponse response = service.create(
                new FeedbackRequest(5L, 4, "Helpful project"), "amina");

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.project().title()).isEqualTo("Clean Water");
        assertThat(response.author().username()).isEqualTo("amina");

        // The author stored on the entity is the authenticated user, not anything
        // the client could have spoofed via the request body.
        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAuthor().getUsername()).isEqualTo("amina");
    }

    @Test
    void create_throwsWhenProjectMissing() {
        when(projectRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new FeedbackRequest(5L, 4, "x"), "amina"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Aid project 5");
        verify(repository, never()).save(any());
    }

    @Test
    void create_throwsWhenAuthorUsernameUnknown() {
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project()));
        when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new FeedbackRequest(5L, 4, "x"), "ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost");
        verify(repository, never()).save(any());
    }

    @Test
    void findById_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Feedback 404");
    }

    @Test
    void delete_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
