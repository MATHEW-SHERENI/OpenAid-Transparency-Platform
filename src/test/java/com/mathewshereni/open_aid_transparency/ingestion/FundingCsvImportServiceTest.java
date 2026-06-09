package com.mathewshereni.open_aid_transparency.ingestion;

import com.mathewshereni.open_aid_transparency.donor.Donor;
import com.mathewshereni.open_aid_transparency.donor.DonorRepository;
import com.mathewshereni.open_aid_transparency.donor.DonorType;
import com.mathewshereni.open_aid_transparency.funding.FundingFlow;
import com.mathewshereni.open_aid_transparency.funding.FundingFlowRepository;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.recipient.RecipientRepository;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoal;
import com.mathewshereni.open_aid_transparency.sdg.SdgGoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FundingCsvImportService. Verifies the row-level decisions:
 * create when references resolve and the flow is new; skip unknown donor/recipient,
 * duplicates, and malformed rows; and report accurate fetched/created/skipped counts.
 */
@ExtendWith(MockitoExtension.class)
class FundingCsvImportServiceTest {

    @Mock private DonorRepository donorRepository;
    @Mock private RecipientRepository recipientRepository;
    @Mock private FundingFlowRepository fundingFlowRepository;
    @Mock private SdgGoalRepository sdgGoalRepository;

    @InjectMocks
    private FundingCsvImportService service;

    private final Donor gates = Donor.builder().id(1L).name("Gates Foundation").type(DonorType.FOUNDATION).build();
    private final Recipient kenya = Recipient.builder().id(2L).countryName("Kenya").isoCode("KEN").build();

    private static final String HEADER = "donor,recipientIso,year,amount,currency\n";

    @Test
    void importsValidRowUppercasingCurrencyAndScalingAmount() {
        when(donorRepository.findByNameIgnoreCase("Gates Foundation")).thenReturn(Optional.of(gates));
        when(recipientRepository.findByIsoCodeIgnoreCase("KEN")).thenReturn(Optional.of(kenya));
        when(fundingFlowRepository.existsByRecipientAndYearAndDonor(kenya, 2023, gates)).thenReturn(false);

        IngestionResult result = service.importCsv(HEADER + "Gates Foundation,KEN,2023,500000.5,usd\n");

        assertThat(result.fetched()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.skipped()).isZero();

        ArgumentCaptor<FundingFlow> saved = ArgumentCaptor.forClass(FundingFlow.class);
        verify(fundingFlowRepository).save(saved.capture());
        assertThat(saved.getValue().getCurrency()).isEqualTo("USD");
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("500000.50");
        assertThat(saved.getValue().getYear()).isEqualTo(2023);
    }

    @Test
    void tagsFlowWithSdgGoalWhenOptionalColumnPresent() {
        SdgGoal cleanWater = SdgGoal.builder().id(6L).goalNumber(6).title("Clean Water").build();
        when(donorRepository.findByNameIgnoreCase("Gates Foundation")).thenReturn(Optional.of(gates));
        when(recipientRepository.findByIsoCodeIgnoreCase("KEN")).thenReturn(Optional.of(kenya));
        when(fundingFlowRepository.existsByRecipientAndYearAndDonor(kenya, 2023, gates)).thenReturn(false);
        when(sdgGoalRepository.findByGoalNumber(6)).thenReturn(Optional.of(cleanWater));

        IngestionResult result = service.importCsv(
                "donor,recipientIso,year,amount,currency,sdgGoal\nGates Foundation,KEN,2023,100,USD,6\n");

        assertThat(result.created()).isEqualTo(1);
        ArgumentCaptor<FundingFlow> saved = ArgumentCaptor.forClass(FundingFlow.class);
        verify(fundingFlowRepository).save(saved.capture());
        assertThat(saved.getValue().getSdgGoal()).isEqualTo(cleanWater);
    }

    @Test
    void columnOrderDoesNotMatter() {
        when(donorRepository.findByNameIgnoreCase("Gates Foundation")).thenReturn(Optional.of(gates));
        when(recipientRepository.findByIsoCodeIgnoreCase("KEN")).thenReturn(Optional.of(kenya));
        when(fundingFlowRepository.existsByRecipientAndYearAndDonor(kenya, 2023, gates)).thenReturn(false);

        IngestionResult result = service.importCsv(
                "currency,amount,year,recipientIso,donor\nUSD,100,2023,KEN,Gates Foundation\n");

        assertThat(result.created()).isEqualTo(1);
    }

    @Test
    void skipsRowWithUnknownDonor() {
        when(donorRepository.findByNameIgnoreCase("Ghost")).thenReturn(Optional.empty());
        lenient().when(recipientRepository.findByIsoCodeIgnoreCase("KEN")).thenReturn(Optional.of(kenya));

        IngestionResult result = service.importCsv(HEADER + "Ghost,KEN,2023,100,USD\n");

        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.created()).isZero();
        verify(fundingFlowRepository, never()).save(any());
    }

    @Test
    void skipsRowWithUnknownRecipient() {
        when(donorRepository.findByNameIgnoreCase("Gates Foundation")).thenReturn(Optional.of(gates));
        when(recipientRepository.findByIsoCodeIgnoreCase("XXX")).thenReturn(Optional.empty());

        IngestionResult result = service.importCsv(HEADER + "Gates Foundation,XXX,2023,100,USD\n");

        assertThat(result.skipped()).isEqualTo(1);
        verify(fundingFlowRepository, never()).save(any());
    }

    @Test
    void skipsAlreadyImportedRow() {
        when(donorRepository.findByNameIgnoreCase("Gates Foundation")).thenReturn(Optional.of(gates));
        when(recipientRepository.findByIsoCodeIgnoreCase("KEN")).thenReturn(Optional.of(kenya));
        when(fundingFlowRepository.existsByRecipientAndYearAndDonor(kenya, 2023, gates)).thenReturn(true);

        IngestionResult result = service.importCsv(HEADER + "Gates Foundation,KEN,2023,100,USD\n");

        assertThat(result.skipped()).isEqualTo(1);
        verify(fundingFlowRepository, never()).save(any());
    }

    @Test
    void skipsMalformedRowButKeepsGoing() {
        when(donorRepository.findByNameIgnoreCase("Gates Foundation")).thenReturn(Optional.of(gates));
        when(recipientRepository.findByIsoCodeIgnoreCase("KEN")).thenReturn(Optional.of(kenya));
        when(fundingFlowRepository.existsByRecipientAndYearAndDonor(kenya, 2023, gates)).thenReturn(false);

        // First row has a non-numeric amount (skipped); second row is valid (created).
        IngestionResult result = service.importCsv(HEADER
                + "Gates Foundation,KEN,2023,NOT_A_NUMBER,USD\n"
                + "Gates Foundation,KEN,2023,100,USD\n");

        assertThat(result.fetched()).isEqualTo(2);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    void rejectsCsvMissingARequiredColumn() {
        // No "currency" column.
        assertThatThrownBy(() -> service.importCsv("donor,recipientIso,year,amount\nGates Foundation,KEN,2023,100\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void rejectsEmptyCsv() {
        assertThatThrownBy(() -> service.importCsv(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
