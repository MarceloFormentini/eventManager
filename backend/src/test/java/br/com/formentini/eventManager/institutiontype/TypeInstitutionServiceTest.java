package br.com.formentini.eventManager.institutiontype;

import br.com.formentini.eventManager.institutiontype.TypeInstitution;
import br.com.formentini.eventManager.institutiontype.TypeInstitutionRepository;
import br.com.formentini.eventManager.institutiontype.TypeInstitutionService;
import br.com.formentini.eventManager.institutiontype.dto.TypeInstitutionRequest;
import br.com.formentini.eventManager.institutiontype.dto.TypeInstitutionResponse;
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
class TypeInstitutionServiceTest {

    @Mock
    private TypeInstitutionRepository repository;

    @InjectMocks
    private TypeInstitutionService service;

    private TypeInstitution institution;

    @BeforeEach
    void setUp() {
        institution = new TypeInstitution("Universidade");
    }

    @Test
    void shouldCreateTypeInstitution() {
        TypeInstitutionRequest request =
                new TypeInstitutionRequest("Universidade");

        when(repository.save(any(TypeInstitution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TypeInstitutionResponse response = service.create(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Universidade");

        verify(repository).save(any(TypeInstitution.class));
    }

    @Test
    void shouldReturnAllTypeInstitutions() {
        when(repository.findAll()).thenReturn(List.of(
                new TypeInstitution("Universidade"),
                new TypeInstitution("Escola")
        ));

        List<TypeInstitutionResponse> response = service.findAll();

        assertThat(response).hasSize(2);
        assertThat(response)
                .extracting(TypeInstitutionResponse::name)
                .containsExactly("Universidade", "Escola");

        verify(repository).findAll();
    }

    @Test
    void shouldFindTypeInstitutionById() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(institution));

        TypeInstitutionResponse response = service.findById(1L);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Universidade");

        verify(repository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenTypeInstitutionDoesNotExist() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository).findById(99L);
    }

    @Test
    void shouldUpdateTypeInstitution() {
        TypeInstitutionRequest request =
                new TypeInstitutionRequest("Faculdade");

        when(repository.findById(1L))
                .thenReturn(Optional.of(institution));

        when(repository.save(any(TypeInstitution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TypeInstitutionResponse response = service.update(1L, request);

        assertThat(response.name()).isEqualTo("Faculdade");

        verify(repository).findById(1L);
        verify(repository).save(institution);
    }

    @Test
    void shouldDeleteTypeInstitution() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(institution));

        service.delete(1L);

        verify(repository).findById(1L);
        verify(repository).delete(institution);
    }

    @Test
    void shouldNotDeleteNonexistentTypeInstitution() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any(TypeInstitution.class));
    }
}

