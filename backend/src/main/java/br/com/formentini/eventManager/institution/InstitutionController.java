
package br.com.formentini.eventManager.institution;

import br.com.formentini.eventManager.institution.dto.InstitutionRequest;
import br.com.formentini.eventManager.institution.dto.InstitutionResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public List<InstitutionResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public InstitutionResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/type/{typeInstitutionId}")
    public List<InstitutionResponse> findByTypeInstitution(
            @PathVariable Long typeInstitutionId) {
        return service.findByTypeInstitution(typeInstitutionId);
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
