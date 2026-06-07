package com.mathewshereni.open_aid_transparency.recipient;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {

    boolean existsByCountryNameIgnoreCase(String countryName);

    boolean existsByIsoCodeIgnoreCase(String isoCode);
}
