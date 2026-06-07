package com.mathewshereni.open_aid_transparency.user;

import com.mathewshereni.open_aid_transparency.common.exception.DuplicateResourceException;
import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;   // the BCrypt bean from SecurityConfig

    @Transactional(readOnly = true)
    public List<AppUserResponse> findAll() {
        return repository.findAll(Sort.by("username"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppUserResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public AppUserResponse create(AppUserRequest request) {
        if (repository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken.");
        }
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already registered.");
        }

        AppUser user = AppUser.builder()
                .username(request.username())
                .email(request.email())
                // Hash the password ONE WAY before storing. The raw password is never saved.
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .build();

        return toResponse(repository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private AppUser getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found."));
    }

    /** Note: passwordHash is deliberately NOT copied into the response. */
    private AppUserResponse toResponse(AppUser u) {
        return new AppUserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getRole(),
                u.isEnabled(),
                u.getCreatedAt());
    }
}
