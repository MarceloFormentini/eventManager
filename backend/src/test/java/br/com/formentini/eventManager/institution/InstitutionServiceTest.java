package br.com.formentini.eventManager.institution;

import br.com.formentini.eventManager.institution.dto.InstitutionRequest;
import br.com.formentini.eventManager.institution.dto.InstitutionResponse;
import br.com.formentini.eventManager.institutiontype.TypeInstitution;
import br.com.formentini.eventManager.institutiontype.TypeInstitutionRepository;
import br.com.formentini.eventManager.shared.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

    @Mock
    private InstitutionRepository repository;

    @Mock
    private TypeInstitutionRepository typeRepository;

    @InjectMocks
    private InstitutionService service;

    private TypeInstitution typeInstitution;
    private Institution institution;

    @BeforeEach
    void setUp() {
        typeInstitution = new TypeInstitution("Universidade");
        institution = new Institution(typeInstitution, "Universidade Estadual");
    }

    @Test
    void shouldCreateInstitution() {
        InstitutionRequest request =
                new InstitutionRequest("Universidade Estadual", 1L);

        when(typeRepository.findById(1L))
                .thenReturn(Optional.of(typeInstitution));

        when(repository.save(any(Institution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InstitutionResponse response = service.create(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Universidade Estadual");
        assertThat(response.typeInstitutionName()).isEqualTo("Universidade");

        verify(typeRepository).findById(1L);
        verify(repository).save(any(Institution.class));
    }

    @Test
    void shouldNotCreateInstitutionWhenTypeDoesNotExist() {
        InstitutionRequest request =
                new InstitutionRequest("Universidade Estadual", 99L);

        when(typeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(typeRepository).findById(99L);
        verify(repository, never()).save(any(Institution.class));
    }

    @Test
    void shouldReturnAllInstitutions() {
        when(repository.findAll())
                .thenReturn(List.of(institution));

        List<InstitutionResponse> response = service.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name())
                .isEqualTo("Universidade Estadual");
        assertThat(response.get(0).typeInstitutionName())
                .isEqualTo("Universidade");

        verify(repository).findAll();
    }

    @Test
    void shouldFindInstitutionById() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(institution));

        InstitutionResponse response = service.findById(1L);

        assertThat(response.name()).isEqualTo("Universidade Estadual");
        assertThat(response.typeInstitutionId()).isEqualTo(
                typeInstitution.getId()
        );

        verify(repository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenInstitutionDoesNotExist() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository).findById(99L);
    }

    @Test
    void shouldUpdateInstitution() {
        TypeInstitution newType = new TypeInstitution("Escola");

        InstitutionRequest request =
                new InstitutionRequest("Escola Municipal", 2L);

        when(repository.findById(1L))
                .thenReturn(Optional.of(institution));

        when(typeRepository.findById(2L))
                .thenReturn(Optional.of(newType));

        when(repository.save(any(Institution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InstitutionResponse response = service.update(1L, request);

        assertThat(response.name()).isEqualTo("Escola Municipal");
        assertThat(response.typeInstitutionName()).isEqualTo("Escola");

        verify(repository).findById(1L);
        verify(typeRepository).findById(2L);
        verify(repository).save(institution);
    }

    @Test
    void shouldNotUpdateInstitutionWhenNewTypeDoesNotExist() {
        InstitutionRequest request =
                new InstitutionRequest("Escola Municipal", 99L);

        when(repository.findById(1L))
                .thenReturn(Optional.of(institution));

        when(typeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(institution.getName())
                .isEqualTo("Universidade Estadual");

        verify(repository, never()).save(any(Institution.class));
    }

    @Test
    void shouldDeleteInstitution() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(institution));

        service.delete(1L);

        verify(repository).findById(1L);
        verify(repository).delete(institution);
    }

    @Test
    void shouldNotDeleteNonexistentInstitution() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any(Institution.class));
    }

    @Test
    void shouldFindInstitutionsByType() {
        when(typeRepository.findById(1L))
                .thenReturn(Optional.of(typeInstitution));

        when(repository.findByTypeInstitutionId(1L))
                .thenReturn(List.of(institution));

        List<InstitutionResponse> response =
                service.findByTypeInstitution(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name())
                .isEqualTo("Universidade Estadual");

        verify(typeRepository).findById(1L);
        verify(repository).findByTypeInstitutionId(1L);
    }

    @Test
    void shouldNotFindInstitutionsWhenTypeDoesNotExist() {
        when(typeRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByTypeInstitution(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).findByTypeInstitutionId(99L);
    }
}
