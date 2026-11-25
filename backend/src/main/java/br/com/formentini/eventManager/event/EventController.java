package br.com.formentini.eventManager.event;

import br.com.formentini.eventManager.event.dto.EventRequest;
import br.com.formentini.eventManager.event.dto.EventResponse;
import br.com.formentini.eventManager.event.dto.EventStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

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
    public Object findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        if (page == null && size == null) {
            return eventService.findAll();
        }

        int pageNumber = page == null ? 0 : Math.max(page, 0);
        int pageSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        Sort sortOrder = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sort).ascending()
                : Sort.by(sort).descending();

        return eventService.findAll(PageRequest.of(pageNumber, pageSize, sortOrder));
    }

    @GetMapping("/{id}")
    public EventResponse findById(@PathVariable Long id) {
        return eventService.findById(id);
    }

    @GetMapping("/location/{locationId}")
    public List<EventResponse> findByLocation(
            @PathVariable Long locationId
    ) {
        return eventService.findByLocation(locationId);
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