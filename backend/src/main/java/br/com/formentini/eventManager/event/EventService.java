package br.com.formentini.eventManager.event;

import br.com.formentini.eventManager.event.dto.EventRequest;
import br.com.formentini.eventManager.event.dto.EventResponse;
import br.com.formentini.eventManager.event.enums.EventStatus;
import br.com.formentini.eventManager.event.messaging.EventCreated;
import br.com.formentini.eventManager.location.Location;
import br.com.formentini.eventManager.location.LocationRepository;
import br.com.formentini.eventManager.outbox.OutboxService;
import br.com.formentini.eventManager.shared.exception.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final OutboxService outboxService;

    public EventService(
            EventRepository eventRepository,
            LocationRepository locationRepository,
            OutboxService outboxService
    ) {
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        validateDates(request);

        Location location = findLocation(request.locationId());
        validateNoScheduleConflict(request, null);

        Event event = new Event(
                location,
                request.title(),
                request.description(),
                request.startDate(),
                request.endDate(),
                request.status()
        );

        Event savedEvent = eventRepository.save(event);

        EventResponse response = toResponse(savedEvent);

        EventCreated eventCreated = new EventCreated(
                response.id(),
                response.title(),
                response.description(),
                response.startDate(),
                response.endDate(),
                response.status(),
                response.locationId(),
                response.locationName()
        );

        outboxService.save(
                "EVENT",
                response.id(),
                "EventCreated",
                eventCreated
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findAll() {
        return eventRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> findAll(Pageable pageable) {
        return eventRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse findById(Long id) {
        return toResponse(findEvent(id));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findByLocation(Long locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException(
                    "Local não encontrado com o ID: " + locationId
            );
        }

        return eventRepository.findByLocationId(locationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> findByLocation(Long locationId, Pageable pageable) {
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException(
                    "Local não encontrado com o ID: " + locationId
            );
        }

        return eventRepository.findByLocationId(locationId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request) {
        validateDates(request);

        Event event = findEvent(id);
        if (event.getStatus() == EventStatus.CANCELLED
                || event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Eventos cancelados ou concluídos não podem ser editados."
            );
        }
        Location location = findLocation(request.locationId());
        validateNoScheduleConflict(request, id);

        event.update(
                location,
                request.title(),
                request.description(),
                request.startDate(),
                request.endDate()
        );
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse updateStatus(Long id, EventStatus status) {
        Event event = findEvent(id);

        event.changeStatus(status);

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void delete(Long id) {
        Event event = findEvent(id);
        eventRepository.delete(event);
    }

    private Event findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evento não encontrado com o ID: " + id
                ));
    }

    private Location findLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Local não encontrado com o ID: " + id
                ));
    }

    private void validateDates(EventRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException(
                    "A data e hora de término devem ser posteriores às de início."
            );
        }
    }

    private void validateNoScheduleConflict(
            EventRequest request,
            Long excludedEventId
    ) {
        boolean hasConflict = excludedEventId == null
                ? eventRepository
                        .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
                                request.locationId(),
                                request.endDate(),
                                request.startDate()
                        )
                : eventRepository
                        .existsByLocationIdAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
                                request.locationId(),
                                request.endDate(),
                                request.startDate(),
                                excludedEventId
                        );

        if (hasConflict) {
            throw new EventScheduleConflictException(
                    "Já existe outro evento agendado neste local durante o período informado."
            );
        }
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartDate(),
                event.getEndDate(),
                event.getStatus(),
                event.getLocation().getId(),
                event.getLocation().getName(),
                event.getCreatedAt()
        );
    }
}