package br.com.formentini.eventManager.location;

import br.com.formentini.eventManager.institution.Institution;
import br.com.formentini.eventManager.institution.InstitutionRepository;
import br.com.formentini.eventManager.location.dto.LocationRequest;
import br.com.formentini.eventManager.location.dto.LocationResponse;
import br.com.formentini.eventManager.shared.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private InstitutionRepository institutionRepository;

    @InjectMocks
    private LocationService locationService;

    private Institution institution;

    @BeforeEach
    void setUp() {
        institution = mock(Institution.class);
    }

    private void stubInstitutionDetails() {
        when(institution.getId()).thenReturn(1L);
        when(institution.getName()).thenReturn("Universidade Estadual");
    }

    private LocationRequest validRequest() {
        return new LocationRequest(
                "Auditório Principal",
                1L
        );
    }

    private Location location() {
        return new Location(
                institution,
                "Auditório Principal"
        );
    }

    @Test
    void shouldCreateLocation() {
        Location location = location();
        stubInstitutionDetails();

        when(institutionRepository.findById(1L))
                .thenReturn(Optional.of(institution));

        when(locationRepository.save(any(Location.class)))
                .thenReturn(location);

        LocationResponse response =
                locationService.create(validRequest());

        assertThat(response.name()).isEqualTo("Auditório Principal");
        assertThat(response.institutionId()).isEqualTo(1L);
        assertThat(response.institutionName())
                .isEqualTo("Universidade Estadual");

        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void shouldNotCreateLocationWhenInstitutionDoesNotExist() {
        when(institutionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                locationService.create(validRequest())
        ).isInstanceOf(ResourceNotFoundException.class)
         .hasMessageContaining("Instituição não encontrada");

        verify(locationRepository, never()).save(any());
    }

    @Test
    void shouldFindAllLocations() {
        stubInstitutionDetails();

        when(locationRepository.findAll())
                .thenReturn(List.of(location()));

        List<LocationResponse> result =
                locationService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name())
                .isEqualTo("Auditório Principal");
    }

    @Test
    void shouldFindLocationById() {
        stubInstitutionDetails();

        when(locationRepository.findById(1L))
                .thenReturn(Optional.of(location()));

        LocationResponse response =
                locationService.findById(1L);

        assertThat(response.name()).isEqualTo("Auditório Principal");
    }

    @Test
    void shouldThrowWhenLocationDoesNotExist() {
        when(locationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                locationService.findById(99L)
        ).isInstanceOf(ResourceNotFoundException.class)
         .hasMessageContaining("Local não encontrado");
    }

    @Test
    void shouldFindLocationsByInstitution() {
        stubInstitutionDetails();

        when(institutionRepository.findById(1L))
                .thenReturn(Optional.of(institution));

        when(locationRepository.findByInstitutionId(1L))
                .thenReturn(List.of(location()));

        List<LocationResponse> result =
                locationService.findByInstitution(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).institutionId()).isEqualTo(1L);
    }

    @Test
    void shouldNotFindLocationsWhenInstitutionDoesNotExist() {
        when(institutionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                locationService.findByInstitution(99L)
        ).isInstanceOf(ResourceNotFoundException.class);

        verify(locationRepository, never())
                .findByInstitutionId(anyLong());
    }

    @Test
    void shouldUpdateLocation() {
        Location existing = location();
        stubInstitutionDetails();

        when(locationRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(institutionRepository.findById(1L))
                .thenReturn(Optional.of(institution));

        when(locationRepository.save(existing))
                .thenReturn(existing);

        LocationRequest request = new LocationRequest(
                "Novo Auditório",
                1L
        );

        LocationResponse response =
                locationService.update(1L, request);

        assertThat(response.name()).isEqualTo("Novo Auditório");
    }

    @Test
    void shouldNotUpdateWhenLocationDoesNotExist() {
        when(locationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                locationService.update(99L, validRequest())
        ).isInstanceOf(ResourceNotFoundException.class);

        verify(locationRepository, never()).save(any());
    }

    @Test
    void shouldNotUpdateWhenInstitutionDoesNotExist() {
        when(locationRepository.findById(1L))
                .thenReturn(Optional.of(location()));

        when(institutionRepository.findById(99L))
                .thenReturn(Optional.empty());

        LocationRequest request = new LocationRequest(
                "Novo Auditório",
                99L
        );

        assertThatThrownBy(() ->
                locationService.update(1L, request)
        ).isInstanceOf(ResourceNotFoundException.class);

        verify(locationRepository, never()).save(any());
    }

    @Test
    void shouldDeleteLocation() {
        Location existing = location();

        when(locationRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        locationService.delete(1L);

        verify(locationRepository).delete(existing);
    }

    @Test
    void shouldNotDeleteWhenLocationDoesNotExist() {
        when(locationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                locationService.delete(99L)
        ).isInstanceOf(ResourceNotFoundException.class);

        verify(locationRepository, never()).delete(any());
    }
}
