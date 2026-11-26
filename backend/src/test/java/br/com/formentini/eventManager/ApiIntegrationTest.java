package br.com.formentini.eventManager;

import br.com.formentini.eventManager.event.Event;
import br.com.formentini.eventManager.event.EventRepository;
import br.com.formentini.eventManager.event.enums.EventStatus;
import br.com.formentini.eventManager.institution.Institution;
import br.com.formentini.eventManager.institution.InstitutionRepository;
import br.com.formentini.eventManager.institutiontype.TypeInstitution;
import br.com.formentini.eventManager.institutiontype.TypeInstitutionRepository;
import br.com.formentini.eventManager.location.Location;
import br.com.formentini.eventManager.location.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "outbox.dispatcher.fixed-delay-ms=3600000",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("event_manager_api_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static final RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:4-management");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TypeInstitutionRepository typeInstitutionRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from outbox_event");
        jdbcTemplate.update("delete from processed_event");
        jdbcTemplate.update("delete from event");
        jdbcTemplate.update("delete from location");
        jdbcTemplate.update("delete from institution");
        jdbcTemplate.update("delete from type_institution");
    }

    @Test
    void shouldPaginateAllAndFilteredResourceCollections() throws Exception {
        Catalog catalog = createCatalog();
        TypeInstitution anotherType = typeInstitutionRepository.saveAndFlush(
                new TypeInstitution("Escola", "Segundo tipo")
        );
        Institution anotherInstitution = institutionRepository.saveAndFlush(
                new Institution(anotherType, "Escola Municipal")
        );
        locationRepository.saveAndFlush(
                new Location(anotherInstitution, "Sala de eventos")
        );
        persistEvent(catalog.location(), "Evento A", startAt(9), startAt(10));
        persistEvent(catalog.location(), "Evento B", startAt(11), startAt(12));

        assertPage(
                "/api/v1/institution-types?page=0&size=1&sort=name&direction=asc",
                "name", "Escola", 2
        );
        assertPage("/api/v1/institutions?page=0&size=1&sort=name&direction=asc",
                "name", "Escola Municipal", 2);
        assertPage("/api/v1/institutions/type/" + catalog.type().getId()
                        + "?page=0&size=1",
                "name", "Universidade Estadual", 1);
        assertPage("/api/v1/locations?page=0&size=1&sort=name&direction=asc",
                "name", "Auditório Principal", 2);
        assertPage("/api/v1/locations/institution/" + catalog.institution().getId()
                        + "?page=0&size=1",
                "name", "Auditório Principal", 1);
        assertPage("/api/v1/events?page=0&size=1&sort=title&direction=asc",
                "title", "Evento A", 2);
        assertPage("/api/v1/events/location/" + catalog.location().getId()
                        + "?page=0&size=1&sort=title&direction=asc",
                "title", "Evento A", 2);
    }

    @Test
    void shouldEnforceEventRulesAndReturnDocumentedHttpStatuses() throws Exception {
        Catalog catalog = createCatalog();
        String eventRequest = eventRequest(
                catalog.location().getId(),
                "Evento de teste",
                startAt(9),
                startAt(10),
                "DRAFT"
        );

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest(
                                catalog.location().getId(),
                                "Evento em conflito",
                                startAt(9),
                                startAt(10),
                                "PUBLISHED"
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        Long eventId = eventRepository.findAll().getFirst().getId();
        mockMvc.perform(patch("/api/v1/events/{id}/status", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(patch("/api/v1/events/{id}/status", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest(
                                catalog.location().getId(),
                                "Novo evento após cancelamento",
                                startAt(9),
                                startAt(10),
                                "DRAFT"
                        )))
                .andExpect(status().isCreated());

        Long createdEventId = eventRepository.findAll().stream()
                .filter(event ->
                        event.getTitle().equals("Novo evento após cancelamento"))
                .findFirst()
                .orElseThrow()
                .getId();
        mockMvc.perform(delete("/api/v1/events/{id}", createdEventId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(delete("/api/v1/institutions/{id}",
                        catalog.institution().getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    private void assertPage(
            String path,
            String contentField,
            String expectedValue,
            int totalElements
    )
            throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0]." + contentField).value(expectedValue))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(totalElements))
                .andExpect(jsonPath("$.totalPages").value(totalElements))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(totalElements == 1));
    }

    private Catalog createCatalog() {
        TypeInstitution type = typeInstitutionRepository.saveAndFlush(
                new TypeInstitution("Universidade", "Instituição de ensino")
        );
        Institution institution = institutionRepository.saveAndFlush(
                new Institution(type, "Universidade Estadual")
        );
        Location location = locationRepository.saveAndFlush(
                new Location(institution, "Auditório Principal")
        );
        return new Catalog(type, institution, location);
    }

    private Event persistEvent(
            Location location,
            String title,
            OffsetDateTime start,
            OffsetDateTime end
    ) {
        return eventRepository.saveAndFlush(new Event(
                location,
                title,
                "Descrição",
                start,
                end,
                EventStatus.PUBLISHED
        ));
    }

    private OffsetDateTime startAt(int hour) {
        return OffsetDateTime.parse("2027-05-15T" + "%02d".formatted(hour)
                + ":00:00-03:00");
    }

    private String eventRequest(
            Long locationId,
            String title,
            OffsetDateTime start,
            OffsetDateTime end,
            String status
    ) {
        return """
                {
                  "title": "%s",
                  "description": "Teste de contrato",
                  "startDate": "%s",
                  "endDate": "%s",
                  "locationId": %d,
                  "status": "%s"
                }
                """.formatted(title, start, end, locationId, status);
    }

    private record Catalog(
            TypeInstitution type,
            Institution institution,
            Location location
    ) {
    }
}
