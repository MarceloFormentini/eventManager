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
import br.com.formentini.eventManager.outbox.enums.OutboxStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "outbox.dispatcher.fixed-delay-ms=600000",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.dynamic=false"
})
@Testcontainers
class EventReliabilityIntegrationTest {

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
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TypeInstitutionRepository typeInstitutionRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TypeInstitution typeInstitution;
    private Institution institution;
    private Location location;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        eventRepository.deleteAll();
        locationRepository.deleteAll();
        institutionRepository.deleteAll();
        typeInstitutionRepository.deleteAll();

        typeInstitution = typeInstitutionRepository.saveAndFlush(
                new TypeInstitution("Universidade")
        );
        institution = institutionRepository.saveAndFlush(
                new Institution(typeInstitution, "Instituição de confiabilidade")
        );
        location = locationRepository.saveAndFlush(
                new Location(institution, "Auditório de confiabilidade")
        );
    }

    @Test
    void shouldPersistEventAndOutboxRecordInTheSameTransaction() {
        var response = eventService.create(eventRequest(
                "Evento e outbox atomicos",
                OffsetDateTime.parse("2027-06-01T09:00:00-03:00"),
                OffsetDateTime.parse("2027-06-01T12:00:00-03:00")
        ));

        Event savedEvent = eventRepository.findById(response.id()).orElseThrow();
        List<OutboxEvent> outboxRecords = outboxEventRepository.findAll();

        assertThat(savedEvent.getLocation().getId()).isEqualTo(location.getId());
        assertThat(outboxRecords).hasSize(1);
        OutboxEvent outboxEvent = outboxRecords.getFirst();
        assertThat(outboxEvent.getAggregateType()).isEqualTo("EVENT");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(response.id());
        assertThat(outboxEvent.getEventType()).isEqualTo("EventCreated");
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxEvent.getPayload()).contains(
                "\"eventId\":" + response.id(),
                "Evento e outbox atomicos"
        );
    }

    @Test
    void shouldAllowOnlyOneConcurrentReservationForTheSameLocationAndTime()
            throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        OffsetDateTime startDate =
                OffsetDateTime.parse("2027-06-02T09:00:00-03:00");
        OffsetDateTime endDate =
                OffsetDateTime.parse("2027-06-02T12:00:00-03:00");

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() ->
                    createConcurrently(ready, start, "Reserva concorrente 1", startDate, endDate)
            );
            var second = executor.submit(() ->
                    createConcurrently(ready, start, "Reserva concorrente 2", startDate, endDate)
            );

            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Boolean> results = List.of(
                    first.get(15, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(15, java.util.concurrent.TimeUnit.SECONDS)
            );

            assertThat(results).containsExactlyInAnyOrder(true, false);
        } finally {
            start.countDown();
        }

        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectDeletingInstitutionWithDependentLocations() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "delete from institution where id = ?",
                        institution.getId()
                )
        );

        assertThat(institutionRepository.existsById(institution.getId())).isTrue();
        assertThat(locationRepository.existsById(location.getId())).isTrue();
    }

    @Test
    void shouldRejectDeletingLocationWithDependentEvents() {
        eventService.create(eventRequest(
                "Evento dependente",
                OffsetDateTime.parse("2027-06-03T09:00:00-03:00"),
                OffsetDateTime.parse("2027-06-03T12:00:00-03:00")
        ));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "delete from location where id = ?",
                        location.getId()
                )
        );

        assertThat(locationRepository.existsById(location.getId())).isTrue();
    }

    private boolean createConcurrently(
            CountDownLatch ready,
            CountDownLatch start,
            String title,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent test did not start");
        }
        try {
            eventService.create(eventRequest(title, startDate, endDate));
            return true;
        } catch (EventScheduleConflictException | DataIntegrityViolationException exception) {
            return false;
        }
    }

    private EventRequest eventRequest(
            String title,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    ) {
        return new EventRequest(
                title,
                "Integration reliability test",
                startDate,
                endDate,
                location.getId(),
                EventStatus.DRAFT
        );
    }
}
