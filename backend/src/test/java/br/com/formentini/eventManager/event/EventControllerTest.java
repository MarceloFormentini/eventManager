package br.com.formentini.eventManager.event;

import br.com.formentini.eventManager.event.dto.EventRequest;
import br.com.formentini.eventManager.event.dto.EventResponse;
import br.com.formentini.eventManager.event.enums.EventStatus;
import br.com.formentini.eventManager.shared.exception.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private EventResponse response() {
        return new EventResponse(
                1L,
                "Conferência de Tecnologia",
                "Evento sobre tecnologia",
                OffsetDateTime.parse("2027-05-15T09:00:00-03:00"),
                OffsetDateTime.parse("2027-05-15T18:00:00-03:00"),
                EventStatus.DRAFT,
                10L,
                "Auditório Principal",
                OffsetDateTime.parse("2026-11-11T10:00:00-03:00")
        );
    }

    private String validRequestJson() {
        return """
                {
                  "title": "Conferência de Tecnologia",
                  "description": "Evento sobre tecnologia",
                  "startDate": "2027-05-15T09:00:00-03:00",
                  "endDate": "2027-05-15T18:00:00-03:00",
                  "locationId": 10,
                  "status": "DRAFT"
                }
                """;
    }

    @Test
    void shouldCreateEventAndReturn201() throws Exception {
        when(eventService.create(any(EventRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Conferência de Tecnologia"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.locationId").value(10));

        verify(eventService).create(any(EventRequest.class));
    }

    @Test
    void shouldReturn400WhenCreatingEventWithBlankTitle() throws Exception {
        String invalidJson = """
                {
                  "title": " ",
                  "description": "Descrição",
                  "startDate": "2027-05-15T09:00:00-03:00",
                  "endDate": "2027-05-15T18:00:00-03:00",
                  "locationId": 10,
                  "status": "DRAFT"
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        String invalidJson = """
                {
                  "title": "Evento"
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void shouldListAllEvents() throws Exception {
        when(eventService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Conferência de Tecnologia"));
    }

    @Test
    void shouldFindEventById() throws Exception {
        when(eventService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/v1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Conferência de Tecnologia"));
    }

    @Test
    void shouldFindEventsByLocation() throws Exception {
        when(eventService.findByLocation(10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/events/location/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locationId").value(10));
    }

    @Test
    void shouldUpdateEvent() throws Exception {
        EventResponse updated = new EventResponse(
                1L, "Título atualizado", "Nova descrição",
                response().startDate(), response().endDate(),
                EventStatus.DRAFT, 10L, "Auditório Principal",
                response().createdAt());
        when(eventService.update(eq(1L), any(EventRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Título atualizado"));
    }

    @Test
    void shouldUpdateEventStatus() throws Exception {
        EventResponse published = new EventResponse(
                1L, response().title(), response().description(),
                response().startDate(), response().endDate(),
                EventStatus.PUBLISHED, 10L, response().locationName(),
                response().createdAt());
        when(eventService.updateStatus(1L, EventStatus.PUBLISHED))
                .thenReturn(published);

        mockMvc.perform(patch("/api/v1/events/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PUBLISHED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/v1/events/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"UNKNOWN"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void shouldDeleteEventAndReturn204() throws Exception {
        doNothing().when(eventService).delete(1L);

        mockMvc.perform(delete("/api/v1/events/1"))
                .andExpect(status().isNoContent());

        verify(eventService).delete(1L);
    }
}
