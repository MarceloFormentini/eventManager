
package br.com.formentini.eventManager.institution;

import br.com.formentini.eventManager.institution.dto.InstitutionRequest;
import br.com.formentini.eventManager.institution.dto.InstitutionResponse;
import br.com.formentini.eventManager.institutiontype.TypeInstitution;
import br.com.formentini.eventManager.institutiontype.TypeInstitutionRepository;
import br.com.formentini.eventManager.shared.exception.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstitutionService {

    private final InstitutionRepository repository;
    private final TypeInstitutionRepository typeRepository;

    public InstitutionService(
            InstitutionRepository repository,
            TypeInstitutionRepository typeRepository) {
        this.repository = repository;
        this.typeRepository = typeRepository;
    }

    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        TypeInstitution type = findTypeInstitution(request.typeInstitutionId());

        Institution institution = new Institution(type, request.name());

        return toResponse(repository.save(institution));
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstitutionResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public InstitutionResponse update(Long id, InstitutionRequest request) {
        Institution institution = findEntityById(id);
        TypeInstitution type = findTypeInstitution(request.typeInstitutionId());

        institution.setName(request.name());
        institution.setTypeInstitution(type);

        return toResponse(repository.save(institution));
    }

    @Transactional
    public void delete(Long id) {
        Institution institution = findEntityById(id);
        repository.delete(institution);
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponse> findByTypeInstitution(Long typeInstitutionId) {
        findTypeInstitution(typeInstitutionId);

        return repository.findByTypeInstitutionId(typeInstitutionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Institution findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instituição não encontrada: " + id
                ));
    }

    private TypeInstitution findTypeInstitution(Long id) {
        return typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de instituição não encontrado: " + id
                ));
    }

    private InstitutionResponse toResponse(Institution institution) {
        return new InstitutionResponse(
                institution.getId(),
                institution.getName(),
                institution.getTypeInstitution().getId(),
                institution.getTypeInstitution().getName(),
                institution.getCreatedAt()
        );
    }
}
