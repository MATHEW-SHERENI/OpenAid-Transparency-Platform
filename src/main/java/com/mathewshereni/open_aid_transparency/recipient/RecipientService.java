package com.mathewshereni.open_aid_transparency.recipient;

import com.mathewshereni.open_aid_transparency.common.exception.DuplicateResourceException;
import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientRepository repository;

    @Transactional(readOnly = true)
    public List<RecipientResponse> findAll() {
        return repository.findAll(Sort.by("countryName"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecipientResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public RecipientResponse create(RecipientRequest request) {
        if (repository.existsByCountryNameIgnoreCase(request.countryName())) {
            throw new DuplicateResourceException("A recipient '" + request.countryName() + "' already exists.");
        }
        String iso = normaliseIso(request.isoCode());
        if (iso != null && repository.existsByIsoCodeIgnoreCase(iso)) {
            throw new DuplicateResourceException("A recipient with ISO code '" + iso + "' already exists.");
        }
        Recipient recipient = Recipient.builder()
                .countryName(request.countryName())
                .isoCode(iso)
                .region(request.region())
                .build();
        return toResponse(repository.save(recipient));
    }

    @Transactional
    public RecipientResponse update(Long id, RecipientRequest request) {
        Recipient recipient = getOrThrow(id);

        if (!recipient.getCountryName().equalsIgnoreCase(request.countryName())
                && repository.existsByCountryNameIgnoreCase(request.countryName())) {
            throw new DuplicateResourceException("A recipient '" + request.countryName() + "' already exists.");
        }

        String iso = normaliseIso(request.isoCode());
        if (iso != null && !iso.equalsIgnoreCase(recipient.getIsoCode())
                && repository.existsByIsoCodeIgnoreCase(iso)) {
            throw new DuplicateResourceException("A recipient with ISO code '" + iso + "' already exists.");
        }

        recipient.setCountryName(request.countryName());
        recipient.setIsoCode(iso);
        recipient.setRegion(request.region());
        return toResponse(repository.save(recipient));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Recipient getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient " + id + " not found."));
    }

    /** Blank -> null; otherwise upper-cased so "ken" and "KEN" are treated the same. */
    private String normaliseIso(String isoCode) {
        if (isoCode == null || isoCode.isBlank()) {
            return null;
        }
        return isoCode.toUpperCase();
    }

    private RecipientResponse toResponse(Recipient r) {
        return new RecipientResponse(r.getId(), r.getCountryName(), r.getIsoCode(), r.getRegion());
    }
}
