package br.com.formentini.eventManager.event;

import br.com.formentini.eventManager.event.dto.EventRequest;
import br.com.formentini.eventManager.event.dto.EventResponse;
import br.com.formentini.eventManager.event.dto.EventStatusRequest;
import br.com.formentini.eventManager.shared.pagination.PageResponse;
import br.com.formentini.eventManager.shared.pagination.PageableFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventRequest request) {
        return eventService.create(request);
    }

    @GetMapping
    public PageResponse<EventResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return PageResponse.from(eventService.findAll(
                PageableFactory.create(
                        page,
                        size,
                        sort,
                        direction,
                        "id", "createdAt", "title", "startDate", "endDate", "status"
                )
        ));
    }

    @GetMapping("/{id}")
    public EventResponse findById(@PathVariable Long id) {
        return eventService.findById(id);
    }

    @GetMapping("/location/{locationId}")
    public PageResponse<EventResponse> findByLocation(
            @PathVariable Long locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return PageResponse.from(eventService.findByLocation(
                locationId,
                PageableFactory.create(
                        page,
                        size,
                        sort,
                        direction,
                        "id", "createdAt", "title", "startDate", "endDate", "status"
                )
        ));
    }

    @PutMapping("/{id}")
    public EventResponse update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request
    ) {
        return eventService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public EventResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody EventStatusRequest request
    ) {
        return eventService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        eventService.delete(id);
    }
}