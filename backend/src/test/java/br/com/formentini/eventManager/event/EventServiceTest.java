package br.com.formentini.eventManager.event;

import br.com.formentini.eventManager.event.dto.EventRequest;
import br.com.formentini.eventManager.event.dto.EventResponse;
import br.com.formentini.eventManager.event.enums.EventStatus;
import br.com.formentini.eventManager.event.messaging.EventCreated;
import br.com.formentini.eventManager.location.Location;
import br.com.formentini.eventManager.location.LocationRepository;
import br.com.formentini.eventManager.shared.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private EventService eventService;

    private Location location;
    private EventRequest validRequest;
    private OffsetDateTime start;
    private OffsetDateTime end;

    @BeforeEach
    void setUp() {
        location = mock(Location.class);
        lenient().when(location.getId()).thenReturn(10L);
        lenient().when(location.getName()).thenReturn("Auditório Principal");

        start = OffsetDateTime.parse("2027-05-15T09:00:00-03:00");
        end = OffsetDateTime.parse("2027-05-15T18:00:00-03:00");

        validRequest = new EventRequest(
                "Conferência de Tecnologia",
                "Evento sobre tecnologia",
                start,
                end,
                10L,
                EventStatus.DRAFT
        );
    }

    @Test
    void shouldCreateEventWhenRequestIsValidAndThereIsNoScheduleConflict() {
        Event savedEvent = mock(Event.class);
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(eventRepository.existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
                10L, end, start)).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        when(savedEvent.getId()).thenReturn(123L);
        when(savedEvent.getTitle()).thenReturn(validRequest.title());
        when(savedEvent.getDescription()).thenReturn(validRequest.description());
        when(savedEvent.getStartDate()).thenReturn(start);
        when(savedEvent.getEndDate()).thenReturn(end);
        when(savedEvent.getStatus()).thenReturn(EventStatus.DRAFT);
        when(savedEvent.getLocation()).thenReturn(location);

        EventResponse response = eventService.create(validRequest);

        EventCreated expectedEvent = new EventCreated(
                123L,
                validRequest.title(),
                validRequest.description(),
                start,
                end,
                EventStatus.DRAFT,
                10L,
                "Auditório Principal"
        );

        assertEquals("Conferência de Tecnologia", response.title());
        assertEquals(EventStatus.DRAFT, response.status());
        assertEquals(10L, response.locationId());
        assertEquals("Auditório Principal", response.locationName());

        InOrder creationOrder = inOrder(eventRepository, applicationEventPublisher);
        creationOrder.verify(eventRepository).save(any(Event.class));
        creationOrder.verify(applicationEventPublisher).publishEvent(expectedEvent);
    }

    @Test
    void shouldThrowWhenCreatingEventWithEndDateEqualToStartDate() {
        EventRequest request = new EventRequest(
                "Evento", null, start, start, 10L, EventStatus.DRAFT);

        assertThrows(IllegalArgumentException.class,
                () -> eventService.create(request));

        verifyNoInteractions(
                locationRepository,
                eventRepository,
                applicationEventPublisher
        );
    }

    @Test
    void shouldThrowWhenCreatingEventWithEndDateBeforeStartDate() {
        EventRequest request = new EventRequest(
                "Evento", null, end, start, 10L, EventStatus.DRAFT);

        assertThrows(IllegalArgumentException.class,
                () -> eventService.create(request));

        verifyNoInteractions(locationRepository, eventRepository);
    }

    @Test
    void shouldThrowWhenCreatingEventForNonExistingLocation() {
        when(locationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.create(validRequest));

        verify(eventRepository, never())
                .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
                        anyLong(), any(), any());
        verify(eventRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void shouldThrowWhenCreatingEventWithScheduleConflict() {
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(eventRepository.existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
                10L, end, start)).thenReturn(true);

        assertThrows(EventScheduleConflictException.class,
                () -> eventService.create(validRequest));

        verify(eventRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void shouldFindAllEvents() {
        Event event = new Event(location, "Evento", "Descrição",
                start, end, EventStatus.PUBLISHED);
        when(eventRepository.findAll()).thenReturn(List.of(event));

        List<EventResponse> result = eventService.findAll();

        assertEquals(1, result.size());
        assertEquals("Evento", result.get(0).title());
        assertEquals(EventStatus.PUBLISHED, result.get(0).status());
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoEvents() {
        when(eventRepository.findAll()).thenReturn(List.of());

        assertTrue(eventService.findAll().isEmpty());
    }

    @Test
    void shouldFindEventById() {
        Event event = new Event(location, "Evento", null,
                start, end, EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        EventResponse response = eventService.findById(1L);

        assertEquals("Evento", response.title());
        assertEquals(10L, response.locationId());
    }

    @Test
    void shouldThrowWhenEventIdDoesNotExist() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.findById(99L));
    }

    @Test
    void shouldFindEventsByLocation() {
        Event event = new Event(location, "Evento", null,
                start, end, EventStatus.DRAFT);
        when(locationRepository.existsById(10L)).thenReturn(true);
        when(eventRepository.findByLocationId(10L)).thenReturn(List.of(event));

        List<EventResponse> result = eventService.findByLocation(10L);

        assertEquals(1, result.size());
        assertEquals("Evento", result.get(0).title());
        verify(eventRepository).findByLocationId(10L);
    }

    @Test
    void shouldReturnEmptyListWhenLocationHasNoEvents() {
        when(locationRepository.existsById(10L)).thenReturn(true);
        when(eventRepository.findByLocationId(10L)).thenReturn(List.of());

        assertTrue(eventService.findByLocation(10L).isEmpty());
    }

    @Test
    void shouldThrowWhenSearchingEventsForNonExistingLocation() {
        when(locationRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.findByLocation(99L));

        verify(eventRepository, never()).findByLocationId(anyLong());
    }

    @Test
    void shouldUpdateEventWhenThereIsNoConflictWithOtherEvents() {
        Event event = new Event(location, "Título antigo", "Descrição antiga",
                start, end, EventStatus.DRAFT);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(eventRepository
                .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
                        10L, end, start, 1L)).thenReturn(false);
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventRequest request = new EventRequest(
                "Título atualizado", "Nova descrição",
                start, end, 10L, EventStatus.PUBLISHED);

        EventResponse response = eventService.update(1L, request);

        assertEquals("Título atualizado", response.title());
        assertEquals("Nova descrição", response.description());
        assertEquals(EventStatus.DRAFT, response.status(),
                "PUT não deve alterar status; a alteração possui endpoint próprio");

        verify(eventRepository)
                .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
                        10L, end, start, 1L);
    }

    @Test
    void shouldNotTreatTheUpdatedEventAsItsOwnScheduleConflict() {
        Event event = new Event(location, "Evento", null,
                start, end, EventStatus.DRAFT);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(eventRepository
                .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
                        10L, end, start, 1L)).thenReturn(false);
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> eventService.update(1L, validRequest));
    }

    @Test
    void shouldThrowWhenUpdatingEventWithConflictAgainstAnotherEvent() {
        Event event = new Event(location, "Evento", null,
                start, end, EventStatus.DRAFT);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(eventRepository
                .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
                        10L, end, start, 1L)).thenReturn(true);

        assertThrows(EventScheduleConflictException.class,
                () -> eventService.update(1L, validRequest));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistingEvent() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.update(99L, validRequest));

        verify(locationRepository, never()).findById(anyLong());
    }

    @Test
    void shouldThrowWhenUpdatingEventWithInvalidDates() {
        EventRequest request = new EventRequest(
                "Evento", null, end, start, 10L, EventStatus.DRAFT);

        assertThrows(IllegalArgumentException.class,
                () -> eventService.update(1L, request));

        verifyNoInteractions(eventRepository, locationRepository);
    }

    @Test
    void shouldThrowWhenUpdatingEventToNonExistingLocation() {
        Event event = new Event(location, "Evento", null,
                start, end, EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        EventRequest request = new EventRequest(
                "Evento", null, start, end, 99L, EventStatus.DRAFT);

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.update(1L, request));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldUpdateEventStatus() {
        Event event = new Event(location, "Evento", null,
                start, end, EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response =
                eventService.updateStatus(1L, EventStatus.PUBLISHED);

        assertEquals(EventStatus.PUBLISHED, response.status());
        verify(eventRepository).save(event);
    }

    @Test
    void shouldThrowWhenUpdatingStatusForNonExistingEvent() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.updateStatus(99L, EventStatus.PUBLISHED));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldDeleteExistingEvent() {
        Event event = new Event(location, "Evento", null,
                start, end, EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertDoesNotThrow(() -> eventService.delete(1L));

        verify(eventRepository).delete(event);
    }

    @Test
    void shouldThrowWhenDeletingNonExistingEvent() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.delete(99L));

        verify(eventRepository, never()).delete(any());
    }
}