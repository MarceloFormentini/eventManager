
package br.com.formentini.eventManager.institutiontype;

import br.com.formentini.eventManager.institutiontype.dto.TypeInstitutionRequest;
import br.com.formentini.eventManager.institutiontype.dto.TypeInstitutionResponse;
import br.com.formentini.eventManager.shared.pagination.PageResponse;
import br.com.formentini.eventManager.shared.pagination.PageableFactory;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/institution-types")
public class TypeInstitutionController {

    private final TypeInstitutionService service;

    public TypeInstitutionController(TypeInstitutionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TypeInstitutionResponse> create(
            @Valid @RequestBody TypeInstitutionRequest request) {

        TypeInstitutionResponse response = service.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TypeInstitutionResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(PageResponse.from(service.findAll(
                PageableFactory.create(
                        page, size, sort, direction, "id", "createdAt", "name"
                )
        )));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TypeInstitutionResponse> findById(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TypeInstitutionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TypeInstitutionRequest request) {

        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}
