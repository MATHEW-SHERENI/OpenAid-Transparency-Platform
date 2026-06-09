package com.mathewshereni.open_aid_transparency.funding;

import com.mathewshereni.open_aid_transparency.donor.Donor;
import com.mathewshereni.open_aid_transparency.donor.DonorType;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice tests for FundingFlowRepository. @DataJpaTest spins up an
 * in-memory H2 database, builds the schema from our @Entity classes, and gives us
 * a TestEntityManager to seed rows. Each test runs in a transaction that is rolled
 * back at the end, so tests never see each other's data.
 *
 * This is the layer where we prove the CUSTOM queries actually execute and return
 * the right shape - something the mocked service tests cannot do.
 */
@DataJpaTest
class FundingFlowRepositoryTest {

    @Autowired
    private FundingFlowRepository repository;

    @Autowired
    private TestEntityManager em;

    private Donor persistDonor(String name) {
        return em.persist(Donor.builder().name(name).type(DonorType.MULTILATERAL).build());
    }

    private Recipient persistRecipient(String country) {
        return em.persist(Recipient.builder().countryName(country).build());
    }

    private void persistFlow(Donor donor, Recipient recipient, String currency, int year, String amount) {
        em.persist(FundingFlow.builder()
                .donor(donor).recipient(recipient)
                .currency(currency).year(year).amount(new BigDecimal(amount))
                .build());
    }

    @Test
    void totalByRecipient_sumsAndGroupsByRecipientAndCurrencyOrderedByAmountDesc() {
        Donor wb = persistDonor("World Bank");
        Recipient kenya = persistRecipient("Kenya");
        Recipient nigeria = persistRecipient("Nigeria");

        // Kenya USD: 100 + 200 = 300 (2 flows)
        persistFlow(wb, kenya, "USD", 2023, "100.00");
        persistFlow(wb, kenya, "USD", 2024, "200.00");
        // Kenya EUR: 50 (1 flow) - different currency must be a separate group
        persistFlow(wb, kenya, "EUR", 2024, "50.00");
        // Nigeria USD: 500 (1 flow) - the largest, so it should sort first
        persistFlow(wb, nigeria, "USD", 2024, "500.00");
        em.flush();
        em.clear();

        List<FundingByRecipient> report = repository.totalByRecipient();

        // 3 groups: (Nigeria,USD), (Kenya,USD), (Kenya,EUR)
        assertThat(report).hasSize(3);

        // Ordered by total amount DESC -> Nigeria/USD (500) first.
        FundingByRecipient first = report.get(0);
        assertThat(first.countryName()).isEqualTo("Nigeria");
        assertThat(first.currency()).isEqualTo("USD");
        assertThat(first.totalAmount()).isEqualByComparingTo("500.00");
        assertThat(first.flowCount()).isEqualTo(1L);

        // The Kenya/USD group aggregated both flows.
        FundingByRecipient kenyaUsd = report.stream()
                .filter(r -> r.countryName().equals("Kenya") && r.currency().equals("USD"))
                .findFirst().orElseThrow();
        assertThat(kenyaUsd.totalAmount()).isEqualByComparingTo("300.00");
        assertThat(kenyaUsd.flowCount()).isEqualTo(2L);
    }

    @Test
    void existsByRecipientAndYearAndDonor_distinguishesPresentFromAbsent() {
        Donor wb = persistDonor("World Bank");
        Recipient kenya = persistRecipient("Kenya");
        persistFlow(wb, kenya, "USD", 2024, "100.00");
        em.flush();

        assertThat(repository.existsByRecipientAndYearAndDonor(kenya, 2024, wb)).isTrue();
        // Same recipient + donor but a different year -> not a duplicate.
        assertThat(repository.existsByRecipientAndYearAndDonor(kenya, 2023, wb)).isFalse();
    }

    @Test
    void totalBySdg_sumsTaggedFlowsByGoalAndExcludesUntagged() {
        Donor wb = persistDonor("World Bank");
        Recipient kenya = persistRecipient("Kenya");
        SdgGoal cleanWater = em.persist(SdgGoal.builder().goalNumber(6).title("Clean Water").build());
        SdgGoal health = em.persist(SdgGoal.builder().goalNumber(3).title("Good Health").build());

        // SDG 6: 100 + 200 = 300 (2 flows); SDG 3: 50 (1 flow); one UNTAGGED flow.
        em.persist(taggedFlow(wb, kenya, 2023, "100.00", cleanWater));
        em.persist(taggedFlow(wb, kenya, 2024, "200.00", cleanWater));
        em.persist(taggedFlow(wb, kenya, 2022, "50.00", health));
        persistFlow(wb, kenya, "USD", 2021, "999.00");   // no SDG tag -> excluded
        em.flush();
        em.clear();

        var report = repository.totalBySdg();

        // Only the two tagged goals, ordered by goal number ascending (3, then 6).
        assertThat(report).hasSize(2);
        assertThat(report.get(0).goalNumber()).isEqualTo(3);
        assertThat(report.get(0).totalAmount()).isEqualByComparingTo("50.00");
        assertThat(report.get(1).goalNumber()).isEqualTo(6);
        assertThat(report.get(1).totalAmount()).isEqualByComparingTo("300.00");
        assertThat(report.get(1).flowCount()).isEqualTo(2L);
    }

    private FundingFlow taggedFlow(Donor donor, Recipient recipient, int year, String amount, SdgGoal goal) {
        return FundingFlow.builder()
                .donor(donor).recipient(recipient)
                .currency("USD").year(year).amount(new BigDecimal(amount))
                .sdgGoal(goal)
                .build();
    }
}
