package com.mathewshereni.open_aid_transparency.user;

import com.mathewshereni.open_aid_transparency.common.exception.DuplicateResourceException;
import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AppUserService. The security-critical behaviours:
 *   - the raw password is HASHED (via the PasswordEncoder) before persistence and
 *     never stored or returned in the clear,
 *   - username and email are each unique,
 *   - the response never carries the password hash.
 */
@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock private AppUserRepository repository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService service;

    private AppUserRequest request() {
        return new AppUserRequest("amina", "amina@x.io", "supersecret", UserRole.CITIZEN);
    }

    @Test
    void create_hashesPasswordAndNeverExposesIt() {
        when(repository.existsByUsernameIgnoreCase("amina")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("amina@x.io")).thenReturn(false);
        when(passwordEncoder.encode("supersecret")).thenReturn("HASHED");
        when(repository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        AppUserResponse response = service.create(request());

        assertThat(response.username()).isEqualTo("amina");
        assertThat(response.enabled()).isTrue();

        // The stored entity holds the HASH, not the raw password.
        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("HASHED");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("supersecret");
        verify(passwordEncoder).encode("supersecret");
    }

    @Test
    void create_rejectsDuplicateUsername() {
        when(repository.existsByUsernameIgnoreCase("amina")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("amina");
        verify(repository, never()).save(any());
        // We must not hash a password for a user we're going to reject.
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void create_rejectsDuplicateEmail() {
        when(repository.existsByUsernameIgnoreCase("amina")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("amina@x.io")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("amina@x.io");
        verify(repository, never()).save(any());
    }

    @Test
    void findById_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User 404");
    }

    @Test
    void delete_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
