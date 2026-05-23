package com.skybook.controller;

import com.skybook.dto.FlightDTO;
import com.skybook.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<List<FlightDTO.Response>> getAll() {
        return ResponseEntity.ok(flightService.getAll());
    }

    @GetMapping("/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<FlightDTO.Response> getById(@PathVariable String id) {
        return ResponseEntity.ok(flightService.getById(id));
    }

    @GetMapping("/search")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<List<FlightDTO.Response>> search(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination) {
        return ResponseEntity.ok(flightService.search(source, destination));
    }

    @PostMapping
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightDTO.Response> create(@Valid @RequestBody FlightDTO.Request req) {
        return ResponseEntity.ok(flightService.create(req));
    }

    @PutMapping("/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightDTO.Response> update(@PathVariable String id,
                                                      @Valid @RequestBody FlightDTO.Request req) {
        return ResponseEntity.ok(flightService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        flightService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
