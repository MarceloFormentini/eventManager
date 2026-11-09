
package br.com.formentini.eventManager.location;

import br.com.formentini.eventManager.location.dto.LocationRequest;
import br.com.formentini.eventManager.location.dto.LocationResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    public ResponseEntity<LocationResponse> create(
            @Valid @RequestBody LocationRequest request
    ) {
        LocationResponse response = locationService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<LocationResponse>> findAll() {
        return ResponseEntity.ok(locationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(locationService.findById(id));
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<List<LocationResponse>> findByInstitution(
            @PathVariable Long institutionId
    ) {
        return ResponseEntity.ok(
                locationService.findByInstitution(institutionId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequest request
    ) {
        return ResponseEntity.ok(
                locationService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        locationService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
