package com.mathewshereni.open_aid_transparency.funding;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/funding-flows")
@RequiredArgsConstructor
public class FundingFlowController {

    private final FundingFlowService service;

    @GetMapping
    public List<FundingFlowResponse> getAll() {
        return service.findAll();
    }

    /** Reporting endpoint: total funding per recipient country (and currency). */
    @GetMapping("/reports/by-recipient")
    public List<FundingByRecipient> totalByRecipient() {
        return service.totalByRecipient();
    }

    @GetMapping("/{id}")
    public FundingFlowResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<FundingFlowResponse> create(@Valid @RequestBody FundingFlowRequest request,
                                                      UriComponentsBuilder uriBuilder) {
        FundingFlowResponse created = service.create(request);
        URI location = uriBuilder.path("/api/funding-flows/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public FundingFlowResponse update(@PathVariable Long id, @Valid @RequestBody FundingFlowRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
