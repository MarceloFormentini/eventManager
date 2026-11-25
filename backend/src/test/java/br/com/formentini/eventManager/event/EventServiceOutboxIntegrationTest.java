
package br.com.formentini.eventManager.event;

import br.com.formentini.eventManager.event.dto.EventRequest;
import br.com.formentini.eventManager.event.enums.EventStatus;
import br.com.formentini.eventManager.institution.Institution;
import br.com.formentini.eventManager.institution.InstitutionRepository;
import br.com.formentini.eventManager.institutiontype.TypeInstitution;
import br.com.formentini.eventManager.institutiontype.TypeInstitutionRepository;
import br.com.formentini.eventManager.location.Location;
import br.com.formentini.eventManager.location.LocationRepository;
import br.com.formentini.eventManager.outbox.OutboxEvent;
import br.com.formentini.eventManager.outbox.OutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Testcontainers
class EventServiceOutboxIntegrationTest {

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
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TypeInstitutionRepository typeInstitutionRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    private Location location;

    @BeforeEach
    void setUp() {
        TypeInstitution typeInstitution =
                typeInstitutionRepository.saveAndFlush(
                        new TypeInstitution("Universidade")
                );

        Institution institution =
                institutionRepository.saveAndFlush(
                        new Institution(
                                typeInstitution,
                                "Universidade de Teste"
                        )
                );

        location = locationRepository.saveAndFlush(
                new Location(institution, "Auditório de Teste")
        );
    }

    @Test
    void shouldRollbackEventCreationWhenOutboxPersistenceFails() {
        long eventsBefore = eventRepository.count();

        EventRequest request = new EventRequest(
                "Evento de integração",
                "Teste de rollback do Transactional Outbox",
                OffsetDateTime.parse("2027-05-15T09:00:00-03:00"),
                OffsetDateTime.parse("2027-05-15T18:00:00-03:00"),
                location.getId(),
                EventStatus.DRAFT
        );

        doThrow(new RuntimeException("Outbox persistence failure"))
                .when(outboxEventRepository)
                .save(any(OutboxEvent.class));

        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Outbox persistence failure");

        assertThat(eventRepository.count())
                .isEqualTo(eventsBefore);
    }
}
