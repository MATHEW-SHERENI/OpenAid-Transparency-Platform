package com.mathewshereni.open_aid_transparency.donor;

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

/**
 * Full CRUD web layer for donors.
 *   GET    /api/donors        list all
 *   GET    /api/donors/{id}   one
 *   POST   /api/donors        create   -> 201 Created
 *   PUT    /api/donors/{id}   update
 *   DELETE /api/donors/{id}   delete   -> 204 No Content
 */
@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private final DonorService service;

    @GetMapping
    public List<DonorResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public DonorResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    /**
     * @RequestBody  = read the JSON body into a DonorRequest.
     * @Valid        = run its validation rules first.
     * On success we return 201 Created plus a Location header pointing at the new
     * resource - the REST-correct way to answer a creation request.
     */
    @PostMapping
    public ResponseEntity<DonorResponse> create(@Valid @RequestBody DonorRequest request,
                                                UriComponentsBuilder uriBuilder) {
        DonorResponse created = service.create(request);
        URI location = uriBuilder.path("/api/donors/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public DonorResponse update(@PathVariable Long id, @Valid @RequestBody DonorRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
