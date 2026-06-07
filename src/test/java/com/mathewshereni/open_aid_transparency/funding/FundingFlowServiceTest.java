package com.mathewshereni.open_aid_transparency.funding;

import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import com.mathewshereni.open_aid_transparency.donor.Donor;
import com.mathewshereni.open_aid_transparency.donor.DonorRepository;
import com.mathewshereni.open_aid_transparency.donor.DonorType;
import com.mathewshereni.open_aid_transparency.project.AidProject;
import com.mathewshereni.open_aid_transparency.project.AidProjectRepository;
import com.mathewshereni.open_aid_transparency.project.ProjectStatus;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.recipient.RecipientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FundingFlowService - our richest service. It depends on FOUR
 * repositories, so we mock all four. The behaviours that matter:
 *   - currency is upper-cased on the way in,
 *   - the optional project is only resolved when a projectId is supplied,
 *   - a missing donor / recipient / project becomes a 404 (ResourceNotFoundException),
 *   - the three related entities are flattened into summaries in the response.
 */
@ExtendWith(MockitoExtension.class)
class FundingFlowServiceTest {

    @Mock private FundingFlowRepository repository;
    @Mock private DonorRepository donorRepository;
    @Mock private RecipientRepository recipientRepository;
    @Mock private AidProjectRepository projectRepository;

    @InjectMocks
    private FundingFlowService service;

    private Donor donor() {
        return Donor.builder().id(1L).name("World Bank").type(DonorType.MULTILATERAL).build();
    }

    private Recipient recipient() {
        return Recipient.builder().id(2L).countryName("Kenya").isoCode("KEN").build();
    }

    private FundingFlowRequest request(Long projectId) {
        return new FundingFlowRequest(
                new BigDecimal("5000000.00"), "usd", 2024, LocalDate.of(2024, 3, 15),
                1L, 2L, projectId);
    }

    @Test
    void create_uppercasesCurrencyAndFlattensRelations() {
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor()));
        when(recipientRepository.findById(2L)).thenReturn(Optional.of(recipient()));
        when(repository.save(any(FundingFlow.class))).thenAnswer(inv -> {
            FundingFlow f = inv.getArgument(0);
            f.setId(50L);
            return f;
        });

        FundingFlowResponse response = service.create(request(null));

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.currency()).isEqualTo("USD");                 // "usd" normalised
        assertThat(response.donor().name()).isEqualTo("World Bank");      // lazy relation flattened
        assertThat(response.recipient().countryName()).isEqualTo("Kenya");
        assertThat(response.project()).isNull();                          // no projectId given

        // The persisted entity stored the upper-cased currency, not the raw input.
        ArgumentCaptor<FundingFlow> saved = ArgumentCaptor.forClass(FundingFlow.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCurrency()).isEqualTo("USD");
        // With no projectId, we must not even query the project repository.
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void create_resolvesProjectWhenProjectIdSupplied() {
        AidProject project = AidProject.builder()
                .id(9L).title("Clean Water for Turkana").status(ProjectStatus.ACTIVE).build();
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor()));
        when(recipientRepository.findById(2L)).thenReturn(Optional.of(recipient()));
        when(projectRepository.findById(9L)).thenReturn(Optional.of(project));
        when(repository.save(any(FundingFlow.class))).thenAnswer(inv -> inv.getArgument(0));

        FundingFlowResponse response = service.create(request(9L));

        assertThat(response.project()).isNotNull();
        assertThat(response.project().id()).isEqualTo(9L);
        assertThat(response.project().title()).isEqualTo("Clean Water for Turkana");
    }

    @Test
    void create_throwsWhenDonorMissing() {
        when(donorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Donor 1");
        verify(repository, never()).save(any());
    }

    @Test
    void create_throwsWhenRecipientMissing() {
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor()));
        when(recipientRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipient 2");
        verify(repository, never()).save(any());
    }

    @Test
    void create_throwsWhenSuppliedProjectMissing() {
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor()));
        when(recipientRepository.findById(2L)).thenReturn(Optional.of(recipient()));
        when(projectRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(9L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Aid project 9");
        verify(repository, never()).save(any());
    }

    @Test
    void update_replacesFieldsAndRelations() {
        FundingFlow existing = FundingFlow.builder()
                .id(50L).amount(new BigDecimal("1.00")).currency("EUR").year(2020)
                .donor(donor()).recipient(recipient()).build();
        when(repository.findById(50L)).thenReturn(Optional.of(existing));
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor()));
        when(recipientRepository.findById(2L)).thenReturn(Optional.of(recipient()));
        when(repository.save(any(FundingFlow.class))).thenAnswer(inv -> inv.getArgument(0));

        FundingFlowResponse response = service.update(50L, request(null));

        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.amount()).isEqualByComparingTo("5000000.00");
        assertThat(response.year()).isEqualTo(2024);
    }

    @Test
    void findById_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Funding flow 404");
    }

    @Test
    void delete_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
