
package br.com.formentini.eventManager.location;

import br.com.formentini.eventManager.institution.Institution;
import br.com.formentini.eventManager.institution.InstitutionRepository;
import br.com.formentini.eventManager.location.dto.LocationRequest;
import br.com.formentini.eventManager.location.dto.LocationResponse;
import br.com.formentini.eventManager.shared.exception.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final InstitutionRepository institutionRepository;

    public LocationService(
            LocationRepository locationRepository,
            InstitutionRepository institutionRepository
    ) {
        this.locationRepository = locationRepository;
        this.institutionRepository = institutionRepository;
    }

    @Transactional
    public LocationResponse create(LocationRequest request) {
        Institution institution =
                findInstitutionById(request.institutionId());

        Location location = new Location(
            institution,
            request.name()
        );

        Location saved = locationRepository.save(location);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> findAll() {
        return locationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse findById(Long id) {
        Location location = findLocationById(id);

        return toResponse(location);
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> findByInstitution(Long institutionId) {

        // Garante que a instituição existe, mesmo quando não possui locais.
        findInstitutionById(institutionId);

        return locationRepository
                .findByInstitutionId(institutionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LocationResponse update(Long id, LocationRequest request) {

        Location location = findLocationById(id);

        Institution institution =
                findInstitutionById(request.institutionId());

        location.setName(request.name());
        location.setInstitution(institution);

        Location updated = locationRepository.save(location);

        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Location location = findLocationById(id);

        locationRepository.delete(location);
    }

    private Location findLocationById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Local não encontrado com ID: " + id
                        )
                );
    }

    private Institution findInstitutionById(Long id) {
        return institutionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Instituição não encontrada com ID: " + id
                        )
                );
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getInstitution().getId(),
                location.getInstitution().getName(),
                location.getCreatedAt()
        );
    }
}
