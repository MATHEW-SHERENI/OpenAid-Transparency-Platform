package com.mathewshereni.open_aid_transparency.donor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice tests for DonorRepository. The derived queries here back the
 * unique-name rule, so the behaviour that matters is case-INSENSITIVE matching.
 */
@DataJpaTest
class DonorRepositoryTest {

    @Autowired
    private DonorRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void existsByNameIgnoreCase_matchesRegardlessOfCase() {
        em.persistAndFlush(Donor.builder().name("World Bank").type(DonorType.MULTILATERAL).build());

        assertThat(repository.existsByNameIgnoreCase("world bank")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("WORLD BANK")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("Gates Foundation")).isFalse();
    }

    @Test
    void findByNameIgnoreCase_returnsTheDonorRegardlessOfCase() {
        em.persistAndFlush(Donor.builder().name("USAID").type(DonorType.COUNTRY).build());

        assertThat(repository.findByNameIgnoreCase("usaid")).isPresent();
        assertThat(repository.findByNameIgnoreCase("unknown")).isEmpty();
    }
}
