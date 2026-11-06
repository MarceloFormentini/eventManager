package br.com.formentini.eventManager.institutiontype;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.dao.DataIntegrityViolationException;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.formentini.eventManager.institutiontype.TypeInstitution;
import br.com.formentini.eventManager.institutiontype.TypeInstitutionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
class TypeInstitutionRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("event_manager_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TypeInstitutionRepository repository;

    @Test
    void shouldSaveTypeInstitution() {
        TypeInstitution institution =
                new TypeInstitution("Universidade");

        TypeInstitution saved = repository.saveAndFlush(institution);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Universidade");
    }

    @Test
    void shouldFindTypeInstitutionById() {
        TypeInstitution saved = repository.saveAndFlush(
                new TypeInstitution("Universidade")
        );

        var result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Universidade");
    }

    @Test
    void shouldReturnAllTypeInstitutions() {
        repository.saveAndFlush(new TypeInstitution("Universidade"));
        repository.saveAndFlush(new TypeInstitution("Escola"));

        var result = repository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(TypeInstitution::getName)
                .containsExactlyInAnyOrder("Universidade", "Escola");
    }

    @Test
    void shouldUpdateTypeInstitution() {
        TypeInstitution saved = repository.saveAndFlush(
                new TypeInstitution("Universidade")
        );

        saved.setName("Faculdade");

        repository.saveAndFlush(saved);

        var result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Faculdade");
    }

    @Test
    void shouldDeleteTypeInstitution() {
        TypeInstitution saved = repository.saveAndFlush(
                new TypeInstitution("Universidade")
        );

        repository.delete(saved);
        repository.flush();

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void shouldRejectNullName() {
        TypeInstitution institution = new TypeInstitution(null);

        assertThatThrownBy(() -> repository.saveAndFlush(institution))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNameLongerThan100Characters() {
        String longName = "A".repeat(101);

        TypeInstitution institution = new TypeInstitution(longName);

        assertThatThrownBy(() -> repository.saveAndFlush(institution))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
