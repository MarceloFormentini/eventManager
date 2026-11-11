package br.com.formentini.eventManager.event;

import br.com.formentini.eventManager.event.dto.EventRequest;
import br.com.formentini.eventManager.event.dto.EventResponse;
import br.com.formentini.eventManager.event.enums.EventStatus;
import br.com.formentini.eventManager.location.Location;
import br.com.formentini.eventManager.location.LocationRepository;
import br.com.formentini.eventManager.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;

    public EventService(
            EventRepository eventRepository,
            LocationRepository locationRepository
    ) {
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
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

        return toResponse(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findAll() {
        return eventRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse findById(Long id) {
        return toResponse(findEvent(id));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findByLocation(Long locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException(
                    "Location not found with id: " + locationId
            );
        }

        return eventRepository.findByLocationId(locationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request) {
        validateDates(request);

        Event event = findEvent(id);
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
                        "Event not found with id: " + id
                ));
    }

    private Location findLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Location not found with id: " + id
                ));
    }

    private void validateDates(EventRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException(
                    "Event end date/time must be after start date/time"
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
                    "Another event is already scheduled at this location "
                            + "during the requested time"
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