
package br.com.formentini.eventManager.location;

import br.com.formentini.eventManager.location.dto.LocationRequest;
import br.com.formentini.eventManager.location.dto.LocationResponse;
import br.com.formentini.eventManager.shared.pagination.PageResponse;
import br.com.formentini.eventManager.shared.pagination.PageableFactory;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<PageResponse<LocationResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(PageResponse.from(locationService.findAll(
                PageableFactory.create(
                        page, size, sort, direction, "id", "createdAt", "name", "capacity"
                )
        )));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(locationService.findById(id));
    }

    @GetMapping("/institution/{institutionId}")
    public ResponseEntity<PageResponse<LocationResponse>> findByInstitution(
            @PathVariable Long institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(PageResponse.from(
                locationService.findByInstitution(
                        institutionId,
                        PageableFactory.create(
                                page, size, sort, direction,
                                "id", "createdAt", "name", "capacity"
                        )
                )
        ));
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
