
package br.com.formentini.eventManager.institution;

import br.com.formentini.eventManager.institution.dto.InstitutionRequest;
import br.com.formentini.eventManager.institution.dto.InstitutionResponse;
import br.com.formentini.eventManager.shared.pagination.PageResponse;
import br.com.formentini.eventManager.shared.pagination.PageableFactory;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionController {

    private final InstitutionService service;

    public InstitutionController(InstitutionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstitutionResponse create(
            @Valid @RequestBody InstitutionRequest request) {
        return service.create(request);
    }

    @GetMapping
    public PageResponse<InstitutionResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return PageResponse.from(service.findAll(PageableFactory.create(
                page, size, sort, direction, "id", "createdAt", "name"
        )));
    }

    @GetMapping("/{id}")
    public InstitutionResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/type/{typeInstitutionId}")
    public PageResponse<InstitutionResponse> findByTypeInstitution(
            @PathVariable Long typeInstitutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return PageResponse.from(service.findByTypeInstitution(
                typeInstitutionId,
                PageableFactory.create(
                        page, size, sort, direction, "id", "createdAt", "name"
                )
        ));
    }

    @PutMapping("/{id}")
    public InstitutionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody InstitutionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
