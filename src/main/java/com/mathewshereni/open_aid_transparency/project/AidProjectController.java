package com.mathewshereni.open_aid_transparency.project;

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
@RequestMapping("/api/aid-projects")
@RequiredArgsConstructor
public class AidProjectController {

    private final AidProjectService service;

    @GetMapping
    public List<AidProjectResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AidProjectResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<AidProjectResponse> create(@Valid @RequestBody AidProjectRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        AidProjectResponse created = service.create(request);
        URI location = uriBuilder.path("/api/aid-projects/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public AidProjectResponse update(@PathVariable Long id, @Valid @RequestBody AidProjectRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
