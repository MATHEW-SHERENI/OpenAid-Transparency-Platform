package com.mathewshereni.open_aid_transparency.funding;

import com.mathewshereni.open_aid_transparency.donor.Donor;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FundingFlowRepository extends JpaRepository<FundingFlow, Long> {

    /** Used by the batch import to avoid inserting a flow we already loaded. */
    boolean existsByRecipientAndYearAndDonor(Recipient recipient, Integer year, Donor donor);

    /**
     * Our first reporting query, written in JPQL (queries over ENTITIES, not raw
     * tables). "SELECT new ...FundingByRecipient(...)" tells Hibernate to build a
     * FundingByRecipient record from each grouped row - a "constructor expression".
     *
     * SUM/COUNT + GROUP BY do the aggregation inside the database (fast), so we
     * only carry the small summarised result back into Java.
     */
    @Query("""
            SELECT new com.mathewshereni.open_aid_transparency.funding.FundingByRecipient(
                       r.id, r.countryName, f.currency, SUM(f.amount), COUNT(f))
            FROM FundingFlow f
            JOIN f.recipient r
            GROUP BY r.id, r.countryName, f.currency
            ORDER BY SUM(f.amount) DESC
            """)
    List<FundingByRecipient> totalByRecipient();
}
