package com.mathewshereni.open_aid_transparency.recipient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {

    boolean existsByCountryNameIgnoreCase(String countryName);

    boolean existsByIsoCodeIgnoreCase(String isoCode);

    /** Resolve a recipient from a World Bank ISO3 code during funding ingestion. */
    Optional<Recipient> findByIsoCodeIgnoreCase(String isoCode);
}
