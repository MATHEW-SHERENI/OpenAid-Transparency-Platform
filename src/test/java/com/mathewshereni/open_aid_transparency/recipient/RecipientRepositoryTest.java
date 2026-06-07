package com.mathewshereni.open_aid_transparency.recipient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice tests for RecipientRepository. Two independent uniqueness
 * checks (country name and ISO code), both case-insensitive.
 */
@DataJpaTest
class RecipientRepositoryTest {

    @Autowired
    private RecipientRepository repository;

    @Autowired
    private TestEntityManager em;

    @Test
    void existsByCountryNameIgnoreCase_matchesRegardlessOfCase() {
        em.persistAndFlush(Recipient.builder().countryName("Kenya").isoCode("KEN").build());

        assertThat(repository.existsByCountryNameIgnoreCase("kenya")).isTrue();
        assertThat(repository.existsByCountryNameIgnoreCase("Nigeria")).isFalse();
    }

    @Test
    void existsByIsoCodeIgnoreCase_matchesRegardlessOfCase() {
        em.persistAndFlush(Recipient.builder().countryName("Kenya").isoCode("KEN").build());

        assertThat(repository.existsByIsoCodeIgnoreCase("ken")).isTrue();
        assertThat(repository.existsByIsoCodeIgnoreCase("NGA")).isFalse();
    }
}
