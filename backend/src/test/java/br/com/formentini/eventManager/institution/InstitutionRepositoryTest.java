package br.com.formentini.eventManager.institution;

import br.com.formentini.eventManager.institutiontype.TypeInstitution;
import br.com.formentini.eventManager.institutiontype.TypeInstitutionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.dao.DataIntegrityViolationException;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
class InstitutionRepositoryTest {

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
    private InstitutionRepository repository;

    @Autowired
    private TypeInstitutionRepository typeRepository;

    private TypeInstitution typeInstitution;

    @BeforeEach
    void setUp() {
        typeInstitution = typeRepository.saveAndFlush(
                new TypeInstitution("Universidade")
        );
    }

    @Test
    void shouldSaveInstitution() {
        Institution institution =
                new Institution(typeInstitution, "Universidade Estadual");

        Institution saved = repository.saveAndFlush(institution);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Universidade Estadual");
        assertThat(saved.getTypeInstitution().getId())
                .isEqualTo(typeInstitution.getId());
    }

    @Test
    void shouldFindInstitutionById() {
        Institution saved = repository.saveAndFlush(
                new Institution(typeInstitution, "Universidade Estadual")
        );

        var result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName())
                .isEqualTo("Universidade Estadual");
        assertThat(result.get().getTypeInstitution().getId())
                .isEqualTo(typeInstitution.getId());
    }

    @Test
    void shouldReturnAllInstitutions() {
        repository.saveAndFlush(
                new Institution(typeInstitution, "Universidade Estadual")
        );

        repository.saveAndFlush(
                new Institution(typeInstitution, "Universidade Federal")
        );

        var result = repository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Institution::getName)
                .containsExactlyInAnyOrder(
                        "Universidade Estadual",
                        "Universidade Federal"
                );
    }

    @Test
    void shouldUpdateInstitution() {
        Institution saved = repository.saveAndFlush(
                new Institution(typeInstitution, "Universidade Estadual")
        );

        saved.setName("Universidade Federal");

        repository.saveAndFlush(saved);

        var result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName())
                .isEqualTo("Universidade Federal");
    }

    @Test
    void shouldDeleteInstitution() {
        Institution saved = repository.saveAndFlush(
                new Institution(typeInstitution, "Universidade Estadual")
        );

        repository.delete(saved);
        repository.flush();

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void shouldFindInstitutionsByTypeInstitutionId() {
        repository.saveAndFlush(
                new Institution(typeInstitution, "Universidade Estadual")
        );

        repository.saveAndFlush(
                new Institution(typeInstitution, "Universidade Federal")
        );

        var result = repository.findByTypeInstitutionId(
                typeInstitution.getId()
        );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Institution::getName)
                .containsExactlyInAnyOrder(
                        "Universidade Estadual",
                        "Universidade Federal"
                );
    }

    @Test
    void shouldRejectInstitutionWithoutType() {
        Institution institution =
                new Institution(null, "Universidade Estadual");

        assertThatThrownBy(() -> repository.saveAndFlush(institution))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
 