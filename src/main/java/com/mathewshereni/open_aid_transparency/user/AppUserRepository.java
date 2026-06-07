package com.mathewshereni.open_aid_transparency.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    /** Used by Phase 3 (login) to look a user up by their username. */
    Optional<AppUser> findByUsernameIgnoreCase(String username);
}
