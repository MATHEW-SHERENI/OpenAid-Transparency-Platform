package com.mathewshereni.open_aid_transparency.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice tests for AppUserRepository. Username and email each have a
 * case-insensitive existence check (registration uniqueness), and login looks a
 * user up by username case-insensitively.
 */
@DataJpaTest
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository repository;

    @Autowired
    private TestEntityManager em;

    private void persistUser(String username, String email) {
        em.persistAndFlush(AppUser.builder()
                .username(username).email(email)
                .passwordHash("hash").role(UserRole.CITIZEN).enabled(true)
                .build());
    }

    @Test
    void existsByUsernameIgnoreCase_matchesRegardlessOfCase() {
        persistUser("Amina", "amina@x.io");

        assertThat(repository.existsByUsernameIgnoreCase("amina")).isTrue();
        assertThat(repository.existsByUsernameIgnoreCase("AMINA")).isTrue();
        assertThat(repository.existsByUsernameIgnoreCase("bob")).isFalse();
    }

    @Test
    void existsByEmailIgnoreCase_matchesRegardlessOfCase() {
        persistUser("Amina", "Amina@X.io");

        assertThat(repository.existsByEmailIgnoreCase("amina@x.io")).isTrue();
        assertThat(repository.existsByEmailIgnoreCase("other@x.io")).isFalse();
    }

    @Test
    void findByUsernameIgnoreCase_returnsUserForLogin() {
        persistUser("Amina", "amina@x.io");

        assertThat(repository.findByUsernameIgnoreCase("AMINA")).isPresent();
        assertThat(repository.findByUsernameIgnoreCase("ghost")).isEmpty();
    }
}
