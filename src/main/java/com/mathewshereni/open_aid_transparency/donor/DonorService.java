package com.mathewshereni.open_aid_transparency.donor;

import com.mathewshereni.open_aid_transparency.common.exception.DuplicateResourceException;
import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for donors. @Transactional makes each method one all-or-nothing
 * database transaction: if anything fails midway, every change is rolled back.
 * readOnly = true is a small optimisation hint for pure reads.
 */
@Service
@RequiredArgsConstructor
public class DonorService {

    private final DonorRepository repository;

    @Transactional(readOnly = true)
    public List<DonorResponse> findAll() {
        return repository.findAll(Sort.by("name"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DonorResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public DonorResponse create(DonorRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A donor named '" + request.name() + "' already exists.");
        }
        Donor donor = Donor.builder()
                .name(request.name())
                .type(request.type())
                .country(request.country())
                .build();
        return toResponse(repository.save(donor));
    }

    @Transactional
    public DonorResponse update(Long id, DonorRequest request) {
        Donor donor = getOrThrow(id);
        // Allow keeping the same name, but block renaming onto ANOTHER donor's name.
        boolean nameChanged = !donor.getName().equalsIgnoreCase(request.name());
        if (nameChanged && repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A donor named '" + request.name() + "' already exists.");
        }
        donor.setName(request.name());
        donor.setType(request.type());
        donor.setCountry(request.country());
        return toResponse(repository.save(donor));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Donor getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donor " + id + " not found."));
    }

    private DonorResponse toResponse(Donor d) {
        return new DonorResponse(d.getId(), d.getName(), d.getType(), d.getCountry());
    }
}
