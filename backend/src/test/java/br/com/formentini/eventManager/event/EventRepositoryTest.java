package br.com.formentini.eventManager.event;

import br.com.formentini.eventManager.event.enums.EventStatus;
import br.com.formentini.eventManager.institution.Institution;
import br.com.formentini.eventManager.institutiontype.TypeInstitution;
import br.com.formentini.eventManager.location.Location;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class EventRepositoryTest {

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
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    private Location location;
    private Location anotherLocation;
    private OffsetDateTime start;
    private OffsetDateTime end;

    @BeforeEach
    void setUp() {
        TypeInstitution type = new TypeInstitution("Universidade");
        entityManager.persist(type);

        Institution institution = new Institution(type, "Instituição de Teste");
        entityManager.persist(institution);

        location = new Location(
                institution, "Auditório Principal");
        anotherLocation = new Location(
                institution, "Sala 2");

        entityManager.persist(location);
        entityManager.persist(anotherLocation);
        entityManager.flush();

        start = OffsetDateTime.parse("2027-05-15T09:00:00-03:00");
        end = OffsetDateTime.parse("2027-05-15T18:00:00-03:00");
    }

    private Event persistEvent(
            Location eventLocation,
            String title,
            OffsetDateTime eventStart,
            OffsetDateTime eventEnd,
            EventStatus status
    ) {
        Event event = new Event(
                eventLocation, title, "Descrição", eventStart, eventEnd, status);
        entityManager.persist(event);
        entityManager.flush();
        return event;
    }

    @Test
    void shouldSaveAndFindEventById() {
        Event saved = eventRepository.save(new Event(
                location, "Conferência", "Descrição",
                start, end, EventStatus.PUBLISHED));

        entityManager.flush();
        entityManager.clear();

        Event found = eventRepository.findById(saved.getId()).orElseThrow();

        assertEquals("Conferência", found.getTitle());
        assertEquals(EventStatus.PUBLISHED, found.getStatus());
        assertEquals(location.getId(), found.getLocation().getId());
        assertNotNull(found.getCreatedAt());
    }

    @Test
    void shouldFindEventsByLocationId() {
        persistEvent(location, "Evento 1", start, end, EventStatus.DRAFT);
        persistEvent(anotherLocation, "Evento 2", start, end, EventStatus.DRAFT);

        List<Event> result = eventRepository.findByLocationId(location.getId());

        assertEquals(1, result.size());
        assertEquals("Evento 1", result.get(0).getTitle());
    }

    @Test
    void shouldReturnEmptyListWhenLocationHasNoEvents() {
        assertTrue(eventRepository.findByLocationId(location.getId()).isEmpty());
    }

    @Test
    void shouldFindEventsByInstitutionId() {
        persistEvent(location, "Evento 1", start, end, EventStatus.DRAFT);
        persistEvent(anotherLocation, "Evento 2", start, end, EventStatus.PUBLISHED);

        List<Event> result =
                eventRepository.findByLocationInstitutionId(
                        location.getInstitution().getId());

        assertEquals(2, result.size());
    }

    @Test
    void shouldDetectScheduleConflictWhenExistingEventOverlapsRequestedPeriod() {
        persistEvent(location, "Evento existente", start, end, EventStatus.PUBLISHED);

        boolean conflict =
                eventRepository
                        .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
                                location.getId(),
                                end.minusHours(1),
                                start.plusHours(1));

        assertTrue(conflict);
    }

    @Test
    void shouldDetectConflictWhenRequestedPeriodContainsExistingEvent() {
        persistEvent(location, "Evento existente",
                start.plusHours(2), end.minusHours(2), EventStatus.PUBLISHED);

        boolean conflict =
                eventRepository
                        .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
                                location.getId(),
                                end,
                                start);

        assertTrue(conflict);
    }

    @Test
    void shouldDetectConflictWhenRequestedPeriodIsInsideExistingEvent() {
        persistEvent(location, "Evento existente", start, end, EventStatus.PUBLISHED);

        boolean conflict =
                eventRepository
                        .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
                                location.getId(),
                                end.minusHours(2),
                                start.plusHours(2));

        assertTrue(conflict);
    }

    @Test
    void shouldNotDetectConflictForEventsAtDifferentLocations() {
        persistEvent(anotherLocation, "Evento em outro local",
                start, end, EventStatus.PUBLISHED);

        boolean conflict =
                eventRepository
                        .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
                                location.getId(), end, start);

        assertFalse(conflict);
    }

    @Test
    void shouldNotDetectConflictForConsecutiveEvents() {
        persistEvent(location, "Evento anterior",
                start, end, EventStatus.PUBLISHED);

        boolean conflict =
                eventRepository
                        .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
                                location.getId(),
                                end.plusHours(2),
                                end);

        assertFalse(conflict);
    }

    @Test
    void shouldExcludeCurrentEventWhenCheckingConflictDuringUpdate() {
        Event event = persistEvent(
                location, "Evento atual", start, end, EventStatus.DRAFT);

        boolean conflict =
                eventRepository
                        .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
                                location.getId(),
                                end,
                                start,
                                event.getId());

        assertFalse(conflict);
    }

    @Test
    void shouldDetectAnotherEventWhenCurrentEventIsExcluded() {
        Event current = persistEvent(
                location, "Evento atual", start, end, EventStatus.DRAFT);
        persistEvent(location, "Outro evento",
                start.plusHours(1), end.plusHours(1), EventStatus.PUBLISHED);

        boolean conflict =
                eventRepository
                        .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
                                location.getId(),
                                end.plusHours(2),
                                start.plusMinutes(30),
                                current.getId());

        assertTrue(conflict);
    }

    @Test
    void shouldPersistEnumAsStringAndReadItBack() {
        Event saved = persistEvent(
                location, "Evento publicado", start, end, EventStatus.PUBLISHED);

        entityManager.clear();

        Event found = eventRepository.findById(saved.getId()).orElseThrow();

        assertEquals(EventStatus.PUBLISHED, found.getStatus());
    }
}
