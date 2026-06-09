package com.mathewshereni.open_aid_transparency.funding;

import com.mathewshereni.open_aid_transparency.donor.Donor;
import com.mathewshereni.open_aid_transparency.project.AidProject;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single movement of aid money: "Donor X committed AMOUNT to Recipient Y in
 * YEAR, optionally for a specific Project." This is the central fact table that
 * our funding-by-country and funding-by-SDG reports aggregate over.
 */
@Entity
@Table(name = "funding_flows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundingFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


     //The amount of money. BigDecimal (NOT double) so totals stay exact.

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /**
     * ISO 4217 currency code, e.g. "USD", "EUR". Required, exactly 3 letters.
     */
    @Column(nullable = false, length = 3)
    private String currency;


     // The reporting year of the flow, e.g. 2024. Handy for yearly aggregation.

    @Column(name = "flow_year", nullable = false)
    private Integer year;


     // Exact date of the transaction, if known. Optional.

    @Column(name = "transaction_date")
    private LocalDate transactionDate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;


     // Which country received it. Required.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;


     // Which project it funded, IF the flow is tied to a specific project.
     // Optional - some flows are general budget support, not project-specific.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aid_project_id")
    private AidProject project;


     // The sector/category this funding advances, as one of the 17 SDGs (e.g. SDG 6
     // = Clean Water, SDG 3 = Health). Optional - powers the "funding by SDG" report.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sdg_goal_id")
    private SdgGoal sdgGoal;
}
