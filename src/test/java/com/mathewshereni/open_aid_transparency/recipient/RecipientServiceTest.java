package com.mathewshereni.open_aid_transparency.recipient;

import com.mathewshereni.open_aid_transparency.common.exception.DuplicateResourceException;
import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RecipientService. Beyond plain CRUD, this service has two pieces
 * of logic worth pinning down: ISO-code normalisation (blank -> null, else upper-
 * cased) and a SECOND uniqueness rule on the ISO code.
 */
@ExtendWith(MockitoExtension.class)
class RecipientServiceTest {

    @Mock
    private RecipientRepository repository;

    @InjectMocks
    private RecipientService service;

    @Test
    void create_uppercasesIsoAndSaves() {
        when(repository.existsByCountryNameIgnoreCase("Kenya")).thenReturn(false);
        when(repository.existsByIsoCodeIgnoreCase("KEN")).thenReturn(false);
        when(repository.save(any(Recipient.class))).thenAnswer(inv -> {
            Recipient r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        RecipientResponse response = service.create(
                new RecipientRequest("Kenya", "ken", "Eastern Africa"));

        assertThat(response.isoCode()).isEqualTo("KEN");   // lower-case input normalised
        assertThat(response.countryName()).isEqualTo("Kenya");

        ArgumentCaptor<Recipient> saved = ArgumentCaptor.forClass(Recipient.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getIsoCode()).isEqualTo("KEN");
    }

    @Test
    void create_treatsBlankIsoAsNullAndSkipsIsoUniquenessCheck() {
        when(repository.existsByCountryNameIgnoreCase("Kenya")).thenReturn(false);
        when(repository.save(any(Recipient.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipientResponse response = service.create(
                new RecipientRequest("Kenya", "   ", "Eastern Africa"));

        assertThat(response.isoCode()).isNull();
        // A null ISO must NOT trigger the ISO uniqueness query.
        verify(repository, never()).existsByIsoCodeIgnoreCase(any());
    }

    @Test
    void create_rejectsDuplicateCountryName() {
        when(repository.existsByCountryNameIgnoreCase("Kenya")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new RecipientRequest("Kenya", "KEN", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Kenya");
        verify(repository, never()).save(any());
    }

    @Test
    void create_rejectsDuplicateIsoCode() {
        when(repository.existsByCountryNameIgnoreCase("Kenya")).thenReturn(false);
        when(repository.existsByIsoCodeIgnoreCase("KEN")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new RecipientRequest("Kenya", "ken", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("KEN");
        verify(repository, never()).save(any());
    }

    @Test
    void update_allowsKeepingSameNameAndIsoWithoutFalseConflict() {
        Recipient existing = Recipient.builder()
                .id(1L).countryName("Kenya").isoCode("KEN").region("Eastern Africa").build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Recipient.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipientResponse response = service.update(1L,
                new RecipientRequest("kenya", "ken", "East Africa"));

        assertThat(response.region()).isEqualTo("East Africa");
        // Neither uniqueness query should fire when name and ISO are unchanged.
        verify(repository, never()).existsByCountryNameIgnoreCase(any());
        verify(repository, never()).existsByIsoCodeIgnoreCase(any());
    }

    @Test
    void update_rejectsRenamingOntoAnotherRecipient() {
        Recipient existing = Recipient.builder()
                .id(1L).countryName("Kenya").isoCode("KEN").build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCountryNameIgnoreCase("Nigeria")).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new RecipientRequest("Nigeria", "KEN", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Nigeria");
        verify(repository, never()).save(any());
    }

    @Test
    void findById_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void delete_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
