package com.mathewshereni.open_aid_transparency.donor;

import com.mathewshereni.open_aid_transparency.common.exception.DuplicateResourceException;
import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DonorService - the simplest of our CRUD services, so it doubles
 * as the template for the others.
 *
 * There is NO Spring context and NO database here. We replace the repository with
 * a Mockito mock (a programmable stand-in), construct the real service around it,
 * and verify the service's own logic in isolation. That makes these tests run in
 * milliseconds and fail for exactly one reason: a bug in DonorService.
 */
@ExtendWith(MockitoExtension.class)
class DonorServiceTest {

    @Mock                       // a fake DonorRepository whose methods we program with when(...)
    private DonorRepository repository;

    @InjectMocks                // a REAL DonorService, with the mock above injected into it
    private DonorService service;

    // A small helper so each test isn't cluttered building the same request.
    private DonorRequest sampleRequest() {
        return new DonorRequest("Gates Foundation", DonorType.FOUNDATION, "USA");
    }

    // ---------- create ----------

    @Test
    void create_savesNewDonorAndReturnsResponse() {
        // Arrange: no existing donor with this name, and save() echoes back the
        // entity with a DB-assigned id (we simulate the database doing that).
        when(repository.existsByNameIgnoreCase("Gates Foundation")).thenReturn(false);
        when(repository.save(any(Donor.class))).thenAnswer(invocation -> {
            Donor toSave = invocation.getArgument(0);
            toSave.setId(99L);
            return toSave;
        });

        // Act
        DonorResponse response = service.create(sampleRequest());

        // Assert the response we hand back to the controller is correct...
        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.name()).isEqualTo("Gates Foundation");
        assertThat(response.type()).isEqualTo(DonorType.FOUNDATION);
        assertThat(response.country()).isEqualTo("USA");

        // ...and that the entity we actually saved was built from the request.
        // ArgumentCaptor grabs the object that was passed to repository.save().
        ArgumentCaptor<Donor> saved = ArgumentCaptor.forClass(Donor.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Gates Foundation");
        assertThat(saved.getValue().getType()).isEqualTo(DonorType.FOUNDATION);
    }

    @Test
    void create_rejectsDuplicateNameAndNeverSaves() {
        when(repository.existsByNameIgnoreCase("Gates Foundation")).thenReturn(true);

        assertThatThrownBy(() -> service.create(sampleRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Gates Foundation");

        // The important behavioural guarantee: a rejected create writes nothing.
        verify(repository, never()).save(any());
    }

    // ---------- findById ----------

    @Test
    void findById_returnsMappedResponseWhenFound() {
        Donor donor = Donor.builder()
                .id(7L).name("USAID").type(DonorType.COUNTRY).country("USA").build();
        when(repository.findById(7L)).thenReturn(Optional.of(donor));

        DonorResponse response = service.findById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("USAID");
    }

    @Test
    void findById_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }

    // ---------- update ----------

    @Test
    void update_changesFieldsAndReturnsResponse() {
        Donor existing = Donor.builder()
                .id(3L).name("USAID").type(DonorType.COUNTRY).country("USA").build();
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Donor.class))).thenAnswer(inv -> inv.getArgument(0));

        DonorRequest request = new DonorRequest("USAID Renamed", DonorType.NGO, "Kenya");
        DonorResponse response = service.update(3L, request);

        assertThat(response.name()).isEqualTo("USAID Renamed");
        assertThat(response.type()).isEqualTo(DonorType.NGO);
        assertThat(response.country()).isEqualTo("Kenya");
    }

    @Test
    void update_allowsKeepingSameNameWithoutFalseConflict() {
        // Same name (even different case) must NOT trip the duplicate check, since
        // the name belongs to the very donor we're updating.
        Donor existing = Donor.builder()
                .id(3L).name("USAID").type(DonorType.COUNTRY).country("USA").build();
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Donor.class))).thenAnswer(inv -> inv.getArgument(0));

        DonorRequest request = new DonorRequest("usaid", DonorType.COUNTRY, "USA");
        DonorResponse response = service.update(3L, request);

        assertThat(response.name()).isEqualTo("usaid");
        // existsByNameIgnoreCase must not even be consulted when the name is unchanged.
        verify(repository, never()).existsByNameIgnoreCase(any());
    }

    @Test
    void update_rejectsRenamingOntoAnotherDonorsName() {
        Donor existing = Donor.builder()
                .id(3L).name("USAID").type(DonorType.COUNTRY).country("USA").build();
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.existsByNameIgnoreCase("World Bank")).thenReturn(true);

        DonorRequest request = new DonorRequest("World Bank", DonorType.MULTILATERAL, "USA");

        assertThatThrownBy(() -> service.update(3L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("World Bank");
        verify(repository, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void delete_removesExistingDonor() {
        Donor existing = Donor.builder().id(5L).name("GIZ").type(DonorType.COUNTRY).build();
        when(repository.findById(5L)).thenReturn(Optional.of(existing));

        service.delete(5L);

        verify(repository).delete(existing);
    }

    @Test
    void delete_throwsWhenMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    // ---------- findAll ----------

    @Test
    void findAll_mapsEveryDonorToAResponse() {
        when(repository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(
                        Donor.builder().id(1L).name("World Bank").type(DonorType.MULTILATERAL).build(),
                        Donor.builder().id(2L).name("GIZ").type(DonorType.COUNTRY).build()));

        List<DonorResponse> responses = service.findAll();

        assertThat(responses).hasSize(2)
                .extracting(DonorResponse::name)
                .containsExactly("World Bank", "GIZ");
    }
}
