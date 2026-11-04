
package br.com.formentini.eventManager.institutiontype;

import br.com.formentini.eventManager.institutiontype.dto.TypeInstitutionRequest;
import br.com.formentini.eventManager.institutiontype.dto.TypeInstitutionResponse;
import br.com.formentini.eventManager.shared.exception.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TypeInstitutionService {

    private final TypeInstitutionRepository repository;

    public TypeInstitutionService(TypeInstitutionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TypeInstitutionResponse create(TypeInstitutionRequest request) {

        TypeInstitution entity = new TypeInstitution(request.name());

        TypeInstitution saved = repository.save(entity);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TypeInstitutionResponse> findAll() {

        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TypeInstitutionResponse findById(Long id) {

        TypeInstitution entity = findEntityById(id);

        return toResponse(entity);
    }

    @Transactional
    public TypeInstitutionResponse update(
            Long id,
            TypeInstitutionRequest request) {

        TypeInstitution entity = findEntityById(id);

        entity.setName(request.name());

        TypeInstitution updated = repository.save(entity);

        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {

        TypeInstitution entity = findEntityById(id);

        repository.delete(entity);
    }

    private TypeInstitution findEntityById(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de instituição não encontrado: " + id
                ));
    }

    private TypeInstitutionResponse toResponse(TypeInstitution entity) {

        return new TypeInstitutionResponse(
                entity.getId(),
                entity.getName(),
                entity.getCreatedAt()
        );
    }
}
