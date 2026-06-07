package com.mathewshereni.open_aid_transparency.donor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data-access layer for Donor. CRUD comes from JpaRepository; the method below
 * is a derived query used to enforce unique donor names.
 */
public interface DonorRepository extends JpaRepository<Donor, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Donor> findByNameIgnoreCase(String name);
}
