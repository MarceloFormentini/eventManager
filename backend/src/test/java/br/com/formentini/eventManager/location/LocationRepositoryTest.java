
package br.com.formentini.eventManager.location;

import br.com.formentini.eventManager.institution.Institution;
import br.com.formentini.eventManager.institution.InstitutionRepository;
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
class LocationRepositoryTest {

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
    private LocationRepository repository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private TypeInstitutionRepository typeRepository;

    private Institution institution;

    @BeforeEach
    void setUp() {
        TypeInstitution type = typeRepository.saveAndFlush(
                new TypeInstitution("Universidade")
        );

        institution = institutionRepository.saveAndFlush(
                new Institution(type, "Universidade Estadual")
        );
    }

    private Location newLocation(String name) {
        return new Location(
                institution,
                name
        );
    }

    @Test
    void shouldSaveLocation() {
        Location saved = repository.saveAndFlush(
                newLocation("Auditório Principal")
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Auditório Principal");
        assertThat(saved.getInstitution().getId())
                .isEqualTo(institution.getId());
    }

    @Test
    void shouldFindLocationById() {
        Location saved = repository.saveAndFlush(
                newLocation("Auditório Principal")
        );

        var result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName())
                .isEqualTo("Auditório Principal");
    }

    @Test
    void shouldReturnAllLocations() {
        repository.saveAndFlush(newLocation("Auditório Principal"));
        repository.saveAndFlush(newLocation("Sala de Reuniões"));

        var result = repository.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldUpdateLocation() {
        Location saved = repository.saveAndFlush(
                newLocation("Auditório Principal")
        );

        saved.setName("Auditório Central");

        repository.saveAndFlush(saved);

        var result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName())
                .isEqualTo("Auditório Central");
    }

    @Test
    void shouldDeleteLocation() {
        Location saved = repository.saveAndFlush(
                newLocation("Auditório Principal")
        );

        repository.delete(saved);
        repository.flush();

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void shouldFindLocationsByInstitutionId() {
        repository.saveAndFlush(newLocation("Auditório Principal"));
        repository.saveAndFlush(newLocation("Sala de Reuniões"));

        var result = repository.findByInstitutionId(
                institution.getId()
        );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Location::getName)
                .containsExactlyInAnyOrder(
                        "Auditório Principal",
                        "Sala de Reuniões"
                );
    }

    @Test
    void shouldRejectLocationWithoutInstitution() {
        Location location = new Location(
                null,
                "Auditório Principal"
        );

        assertThatThrownBy(() -> repository.saveAndFlush(location))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
