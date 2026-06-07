package com.mathewshereni.open_aid_transparency.ingestion;

import com.mathewshereni.open_aid_transparency.donor.Donor;
import com.mathewshereni.open_aid_transparency.donor.DonorRepository;
import com.mathewshereni.open_aid_transparency.donor.DonorType;
import com.mathewshereni.open_aid_transparency.funding.FundingFlow;
import com.mathewshereni.open_aid_transparency.funding.FundingFlowRepository;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.recipient.RecipientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the ODA batch PROCESSOR - the one component with real decision
 * logic. The @Bean method is just a factory that returns the processor lambda, so
 * we can call it directly with mocked repositories: no Spring, no batch runtime.
 *
 * (The reader/writer are thin wrappers around the client/repository and are
 * exercised by the live end-to-end run instead.)
 */
@ExtendWith(MockitoExtension.class)
class OdaIngestionBatchConfigTest {

    @Mock private DonorRepository donorRepository;
    @Mock private RecipientRepository recipientRepository;
    @Mock private FundingFlowRepository fundingFlowRepository;

    private final Donor aggregateDonor =
            Donor.builder().id(1L).name("ODA aggregate").type(DonorType.MULTILATERAL).build();

    private ItemProcessor<WorldBankOdaPoint, FundingFlow> processor;

    @BeforeEach
    void setUp() {
        when(donorRepository.findById(1L)).thenReturn(Optional.of(aggregateDonor));
        processor = new OdaIngestionBatchConfig()
                .odaProcessor(1L, donorRepository, recipientRepository, fundingFlowRepository);
    }

    @Test
    void process_mapsPointToFundingFlowForKnownNewRecipient() throws Exception {
        Recipient kenya = Recipient.builder().id(2L).countryName("Kenya").isoCode("KEN").build();
        when(recipientRepository.findByIsoCodeIgnoreCase("KEN")).thenReturn(Optional.of(kenya));
        when(fundingFlowRepository.existsByRecipientAndYearAndDonor(kenya, 2020, aggregateDonor))
                .thenReturn(false);

        FundingFlow flow = processor.process(
                new WorldBankOdaPoint("KEN", "Kenya", 2020, new BigDecimal("1234567.5")));

        assertThat(flow).isNotNull();
        assertThat(flow.getRecipient()).isEqualTo(kenya);
        assertThat(flow.getDonor()).isEqualTo(aggregateDonor);
        assertThat(flow.getCurrency()).isEqualTo("USD");
        assertThat(flow.getYear()).isEqualTo(2020);
        // Amount is normalised to 2 decimal places.
        assertThat(flow.getAmount()).isEqualByComparingTo("1234567.50");
    }

    @Test
    void process_discardsPointForUntrackedCountry() throws Exception {
        // A country we don't have in our recipients table (e.g. a non-SSF country).
        when(recipientRepository.findByIsoCodeIgnoreCase("USA")).thenReturn(Optional.empty());

        FundingFlow flow = processor.process(
                new WorldBankOdaPoint("USA", "United States", 2020, new BigDecimal("100")));

        // null tells Spring Batch to filter this item out (it won't be written).
        assertThat(flow).isNull();
    }

    @Test
    void process_discardsAlreadyImportedPoint() throws Exception {
        Recipient kenya = Recipient.builder().id(2L).countryName("Kenya").isoCode("KEN").build();
        when(recipientRepository.findByIsoCodeIgnoreCase("KEN")).thenReturn(Optional.of(kenya));
        // This flow was imported on a previous run -> idempotent skip.
        when(fundingFlowRepository.existsByRecipientAndYearAndDonor(kenya, 2020, aggregateDonor))
                .thenReturn(true);

        FundingFlow flow = processor.process(
                new WorldBankOdaPoint("KEN", "Kenya", 2020, new BigDecimal("999")));

        assertThat(flow).isNull();
    }
}
